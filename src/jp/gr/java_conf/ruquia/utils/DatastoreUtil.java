package jp.gr.java_conf.ruquia.utils;

import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.appengine.api.datastore.Key;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.slim3.datastore.Datastore;
import org.slim3.datastore.ModelRef;
import org.slim3.util.BeanUtil;
import org.slim3.util.StringUtil;

/**
 * <p>データストアを使用したテストを実施する際のサポート機能を提供するクラスです。</p>
 * 
 * @author joe yasuta
 * @author Jun-ya HASEBA
 */
public class DatastoreUtil {

    /**
     * <p>モデルクラスの存在するパッケージ名です。</p>
     */
    // TODO: テストを行うプロジェクトに合わせて書き換えてください。
    private static final String MODEL_PACKAGE = "jp.gr.java_conf.ruquia.spam.model";

    /**
     * <p>Excelで作成したテストデータをデータストアにインポートします。
     * インポートするテストデータは以下のルールに則って作成してください。</p>
     * <ul>
     * <li>シート名をカインド名（モデルクラス名）にします。
     * 複数カインドにデータをインポートする場合は、対象のカインドごとにシートを作成してください。</li>
     * <li>シートの1行目は見出しとし、モデルクラスのフィールド名を列挙します。
     * このとき、フィールドの記述の順番は問いません。
     * また、すべてのフィールドを記述する必要もありません。
     * 見出しに挙げられていないフィールドには一律で {@code null} がセットされます。</li>
     * <li>シートの2行目以降にインポートするデータの内容を記述します。
     * 1行が1件分のデータとなり、行の中での記述の順序は見出しの順番に準じます。
     * なお、一部のオブジェクトについては、下記のような特別なフォーマットで記述する必要があります。
     * <ul>
     * <li>{@link java.util.Date} にセットする値は {@code yyyy-MM-dd'T'hh:mm:ss} 形式で記述してください。</li>
     * <li>{@link Key} にセットする値は {@code key(カインド名,ID文字列)} という形式で記述してください。</li>
     * <li>{@link List} にセットする値は {@code list(要素[,要素]...)} という形式で記述してください。</li>
     * <li>{@link Set} にセットする値は {@code set(要素[,要素]...)} という形式で記述してください。</li>
     * <li>{@link ModelRef} にセットする値は {@code ref(カインド名,ID文字列)} という形式で記述してください。</li>
     * </ul>
     * </li>
     * </ul>
     * 
     * @param filePath Excelファイルのパス
     * @throws Exception テスト時にしか使わないメソッドなので丸投げです。すみません。
     */
    public static void importData(String filePath) throws Exception {
        // 特殊フォーマット解析用の正規表現パターンオブジェクトを生成する
        Pattern keyPattern = Pattern.compile("key\\((.+),(.+)\\)");
        Pattern listPattern = Pattern.compile("list\\((.+)\\)");
        Pattern setPattern = Pattern.compile("set\\((.+)\\)");
        Pattern refPattern = Pattern.compile("ref\\((.+),(.+)\\)");

        // ワークブックオブジェクトを生成する
        POIFSFileSystem fileSystem = new POIFSFileSystem(new FileInputStream(filePath));
        HSSFWorkbook workbook = new HSSFWorkbook(fileSystem);

        // 各シートのデータを読み込む
        int sheetCount = workbook.getNumberOfSheets();
        for (int sheetIndex = 0; sheetIndex < sheetCount; sheetIndex++) {
            // インポート対象モデルのクラスオブジェクトを生成する
            String sheetName = workbook.getSheetName(sheetIndex);
            Class<?> modelClass = Class.forName(MODEL_PACKAGE + "." + sheetName);

            // シートオブジェクトを生成する
            HSSFSheet sheet = workbook.getSheetAt(sheetIndex);

            // 見出しのリストを作成する
            HSSFRow titleRow = sheet.getRow(0);
            int maxColumnNo = titleRow.getLastCellNum();
            List<String> title = new ArrayList<String>(maxColumnNo + 1);
            for (int columnIndex = 0; columnIndex <= maxColumnNo; columnIndex++) {
                HSSFCell cell = titleRow.getCell(columnIndex);
                if (cell != null) {
                    title.add(cell.getStringCellValue());
                } else {
                    title.add(null);
                }
            }

            // 各行のデータを読み込む
            int maxRowNo = sheet.getLastRowNum();
            for (int rowIndex = 1; rowIndex <= maxRowNo; rowIndex++) {
                // 行オブジェクトを生成する
                HSSFRow dataRow = sheet.getRow(rowIndex);
                if (dataRow == null) {
                    continue;
                }

                // インポート対象モデルのオブジェクトを生成する
                Object model = modelClass.newInstance();

                // 各セルのデータを読み込む
                Map<String, Object> dataMap = new HashMap<String, Object>();
                for (int columnIndex = 0; columnIndex <= maxColumnNo; columnIndex++) {
                    // セルオブジェクトを生成する
                    HSSFCell cell = dataRow.getCell(columnIndex);

                    // セルの内容を編集用マップに追加する
                    String fieldName = title.get(columnIndex);
                    if (cell != null) {
                        String value = cell.getStringCellValue();
                        Matcher keyMatcher = keyPattern.matcher(value);
                        Matcher listMatcher = listPattern.matcher(value);
                        Matcher setMatcher = setPattern.matcher(value);
                        Matcher refMatcher = refPattern.matcher(value);
                        if (keyMatcher.matches()) {
                            Class<?> keyModelClass = Class.forName(MODEL_PACKAGE + "." + keyMatcher.group(1));
                            Key key = Datastore.createKey(keyModelClass, keyMatcher.group(2));
                            dataMap.put(fieldName, key);
                        } else if (listMatcher.matches()) {
                            String[] elements = listMatcher.group(1).split(",");
                            List<String> list = Arrays.asList(elements);
                            dataMap.put(fieldName, list);
                        } else if (setMatcher.matches()) {
                            String[] elements = setMatcher.group(1).split(",");
                            Set<String> set = new HashSet<String>(Arrays.asList(elements));
                            dataMap.put(fieldName, set);
                        } else if (refMatcher.matches()) {
                            Class<?> keyModelClass = Class.forName(MODEL_PACKAGE + "." + refMatcher.group(1));
                            Key key = Datastore.createKey(keyModelClass, refMatcher.group(2));
                            ModelRef<?> ref = getModelRef(model, fieldName);
                            ref.setKey(key);
                            dataMap.put(fieldName, ref);
                        } else {
                            dataMap.put(fieldName, value);
                        }
                    } else {
                        if (fieldName != null) {
                            dataMap.put(fieldName, null);
                        }
                    }
                }

                // データストアに格納する
                BeanUtil.copy(dataMap, model);
                Datastore.put(model);
            }
        }
    }

    /**
     * <p>指定されたフィールドを {@code ModelRef} とみなして取得します。</p>
     * 
     * @param model {@code ModelRef} フィールドの取得先となるモデルオブジェクト
     * @param fieldName 取得する {@code ModelRef} のフィールド名
     * @return 取得した {@code ModelRef}
     * @throws Exception テスト時にしか使わないメソッドなので丸投げです。すみません。
     */
    private static ModelRef<?> getModelRef(Object model, String fieldName) throws Exception {
        // 対象モデルのクラスオブジェクトを生成する
        Class<?> modelClass = model.getClass();

        // ModelRefを取得する
        String methodName = "get" + toUpperCaseOnlyFirstChar(fieldName);
        Method method = modelClass.getMethod(methodName, (Class[])null);
        Object value = method.invoke(model, (Object[])null);
        return (ModelRef<?>)value;
    }

    /**
     * <p>指定された文字列の先頭文字を大文字に変換します。</p>
     * 
     * @param target 変換対象の文字列
     * @return 変換結果
     */
    private static String toUpperCaseOnlyFirstChar(String target) {
        // 引数をチェックする
        if (StringUtil.isEmpty(target)) {
            return target;
        }

        // 先頭文字を大文字に変換する
        StringBuilder builder = new StringBuilder();
        builder.append(target.substring(0, 1).toUpperCase());
        builder.append(target.substring(1));
        return builder.toString();
    }

}
