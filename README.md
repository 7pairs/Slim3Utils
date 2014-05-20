# slim3-utils

## はじめに

"Slim3Utils"とは、Slim3でのユニットテストをサポートするユーティリティ集です。
現在のところ"MockUtil"と"DatastoreUtil"の二つの機能を提供しています。

## MockUtil

Slim3アプリケーションのテスト時に、Controllerから呼び出すServiceをモックに差し替えるためのユーティリティです。
テストしたいControllerを継承したクラスを作り、そのコンストラクタでMockUtil.inject()を使用します。
以下のサンプルは、テスト対象がIndexController、serviceというUserServiceのprivateフィールドにモックを差し込む場合の例です。

```
public class IndexWithMockController extends IndexController {

    public IndexWithMockController() {
        super();
        try{
            MockUtil.inject(
                this,
                "service",
                new UserService() {
                    @Override
                    public String getUser(String id) {
                        return null;
                    }

                    @Override
                    public List<String> getAllUser() {
                        return new ArrayList();
                    }
                }
            );
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
```

これでUserServiceのgetUser()、getAllUser()メソッドがテスト用のモックに置き換えられました。
続いて、テストプログラムのソースを修正します。
Controllerのテストプログラム中のtester.start()メソッドを呼んでいる箇所を、先ほど作ったIndexWithMockControllerを呼ぶように書き換えます。
例えば、IndexControllerと同じパッケージ内にIndexWithMockControllerを作った場合、以下のように修正します。

```
tester.start("/indexWithMock");
```

あとはテストを実行するだけです。

## DatastoreUtil

Slim3アプリケーションのテスト時に、テストデータをExcelからインポートするためのユーティリティです。
インポートするExcelファイルは以下のルールに則って作成してください。

* シート名をカインド名（モデルクラス名）にします。
  複数カインドにデータをインポートする場合は、対象のカインドごとにシートを作成してください。
* シートの1行目は見出しとし、モデルクラスのフィールド名を列挙します。
  このとき、フィールドの記述の順番は問いません。
  また、すべてのフィールドを記述する必要もありません。
  見出しに挙げられていないフィールドには一律でnullがセットされます。
* シートの2行目以降にインポートするデータの内容を記述します。
  1行が1件分のデータとなり、行の中での記述の順序は見出しの順番に準じます。

インポートする値については、Excelシートのセルの中にそのまま記述します。
Stringだからダブルクォーテーションで囲む、などといった対応をする必要はありません。
ただし、一部のオブジェクトにセットする値については、下記のような特別なフォーマットで記述する必要があります。

* java.util.Dateにセットする値は yyyy-MM-dd'T'hh:mm:ss 形式で記述してください。
* com.google.appengine.api.datastore.Keyにセットする値は key(カインド名,ID文字列) という形式で記述してください。
* java.util.Listにセットする値は list(要素[,要素]...) という形式で記述してください。
* java.util.Setにセットする値は set(要素[,要素]...) という形式で記述してください。
* org.slim3.datastore.ModelRefにセットする値は ref(カインド名,ID文字列) という形式で記述してください。

テストを実行する際は、作成したExcelのファイルパスを指定してimportData()を呼び出してください。
作成したデータがインポートされます。

