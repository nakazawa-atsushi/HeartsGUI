package jp.ac.kyushuu.ait.arsocket;

import android.app.Application;

/**
 * 本アプリ(ARSocket)のベースとなるApplicationクラス
 */
public class ARSocketApplication extends Application {
    /**
     * 設定値をActivityに関わらず保持するためのフィールド変数
     */
    SettingProperty Property = new SettingProperty();
}
