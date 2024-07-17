package jp.ac.kyushuu.ait.arsocket;

/**
 * ファイルアップロードの進捗を通知するリスナー用インタフェース
 */
public interface OnFileUploadProgressListener {
    /**
     * ファイルアップロードに進捗があったときに呼び出すメソッド
     * @param filename 進捗があった対象のファイル名
     * @param completed_filecount アップロード完了したファイルの数
     * @param completed_size アップロード完了したデータ容量
     * @param all_filecount アップロードの対象となっている全てのファイルの数
     * @param all_size アップロードの対象となっている全てのファイルのデータ容量
     */
    void onCompletedUploadFile(String filename, int completed_filecount, long completed_size, int all_filecount, long all_size);
}
