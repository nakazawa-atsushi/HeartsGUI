package jp.ac.kyushuu.ait.arsocket;

/**
 * 文字列からなる配列の変更を通知するリスナー用インタフェース
 */
public interface OnChangedStringListListener {
    /**
     * 文字列からなる配列が変更された際に呼び出すメソッド
     * @param new_list 変更後の文字列配列
     */
    void onChangedStringList(String[] new_list);
}
