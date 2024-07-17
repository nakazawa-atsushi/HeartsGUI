package jp.ac.kyushuu.ait.arsocket;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

/**
 * ログイン用ダイアログのためのクラス
 */
public class LoginDialogFragment extends DialogFragment {

    /**
     * ダイアログでログインを試行したことを通知するリスナー用インタフェース
     */
    public interface OnLoginListener {
        /**
         * ログイン試行時に呼び出すメソッド
         * @param username ダイアログで入力されたユーザー名
         * @param password ダイアログで入力されたパスワード
         */
        void onLogin(String username, String password);
    }

    /**
     * ログイン試行通知リスナー
     */
    private OnLoginListener onLoginListener = null;

    /**
     * ログイン試行通知リスナーを設定する
     * @param listener 設定するログイン試行通知リスナー
     */
    public void setLoginListener(OnLoginListener listener) {
        onLoginListener = listener;
    }

    /**
     *
     * @param savedInstanceState 最後に保存されたインスタンス、ただし最初の生成時にはnull
     * @return 生成したダイアログ
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        //return super.onCreateDialog(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.dialog_login, null))
                .setPositiveButton(R.string.buttonLogin, (DialogInterface.OnClickListener) (dialogInterface, i) -> {
                    if(onLoginListener != null) {
                        EditText editUsername = getDialog().findViewById(R.id.editUsername);
                        EditText editPassword = getDialog().findViewById(R.id.editPassword);
                        onLoginListener.onLogin(editUsername.getText().toString(), editPassword.getText().toString());
                    }
                })
                .setNegativeButton(R.string.buttonCancel, (DialogInterface.OnClickListener) (dialogInterface, i) -> {
                });

        return builder.create();
    }
}
