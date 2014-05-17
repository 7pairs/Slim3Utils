package jp.gr.java_conf.ruquia.utils;

import java.lang.reflect.Field;

/**
 * <p>モックを使用したテストを実施する際のサポート機能を提供するクラスです。</p>
 * 
 * @author Jun-ya HASEBA
 */
public class MockUtil {

    /**
     * <p>オブジェクトにモックを差し込みます。
     * 差し込み先のフィールドが {@code private} であっても実行可能です。</p>
     * 
     * @param dest モックの差し込み先のオブジェクト
     * @param fieldName モックの差し込み先のフィールド名
     * @param src 差し込むモック
     */
    public static void inject(Object dest, String fieldName, Object src) {
        // 差し込み先のフィールドオブジェクトを生成する
        Class<?> destClass = dest.getClass();
        Field field;
        while (true) {
            try {
                field = destClass.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                destClass = destClass.getSuperclass();
                if (destClass.equals(Object.class)) {
                    throw new IllegalArgumentException("フィールド \"" + fieldName + "\" が存在しません。");
                }
            }
        }

        // モックを差し込む
        field.setAccessible(true);
        try {
            field.set(dest, src);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("フィールド \"" + fieldName + "\" にアクセスすることができません。");
        }
    }

}
