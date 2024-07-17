package jp.ac.kyushuu.ait.arsocket;

/**
 * boolean値を持つパラメータの変更を通知するリスナー用インタフェース
 */
public interface OnSetBooleanValueListener {
    /**
     * boolean値を持つパラメータが変更された際に呼び出すメソッド
     * @param new_value 変更後のboolean値
     */
    void onSetBooleanValue(boolean new_value);
}
