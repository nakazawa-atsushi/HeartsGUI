package jp.ac.kyushuu.ait.arsocket;

/**
 * 文字列を持つパラメータの変更を通知するリスナー用インタフェース
 */
public interface OnSetStringValueListener {
    /**
     * 文字列を持つパラメータが変更された際に呼び出すメソッド
     * @param new_value 変更後の文字列
     */
    void onSetStringValue(String new_value);
}
