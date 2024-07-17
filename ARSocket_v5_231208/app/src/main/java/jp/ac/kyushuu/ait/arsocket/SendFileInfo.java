package jp.ac.kyushuu.ait.arsocket;

import android.net.Uri;

/**
 * 言語追加時の音声ファイル情報を格納するためのクラス
 */
public class SendFileInfo {
    /**
     * ファイル名
     */
    public String fileName;
    /**
     * ファイルサイズ（バイト）
     */
    public long sizeInBytes;
    /**
     * ファイルパス
     */
    public Uri uri;
}
