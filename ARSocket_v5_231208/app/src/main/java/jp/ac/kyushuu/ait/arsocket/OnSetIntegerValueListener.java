package jp.ac.kyushuu.ait.arsocket;

/**
 * 整数値を持つパラメータの変更を通知するリスナー用インタフェース
 */
public interface OnSetIntegerValueListener {
    /**
     * 整数値を持つパラメータが変更された際に呼び出すメソッド
     * @param new_value 変更後の整数値
     */
    void onSetIntegerValue(int new_value);
}
