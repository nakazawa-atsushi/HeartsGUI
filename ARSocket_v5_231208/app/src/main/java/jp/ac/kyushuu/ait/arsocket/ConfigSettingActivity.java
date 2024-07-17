package jp.ac.kyushuu.ait.arsocket;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 詳細設定アクティビティクラス
 * 評価データのアップロード、顔モデル、言語、ビープモード、難易度、OpenAI/Azure用設定を行う
 */
public class ConfigSettingActivity extends AppCompatActivity {
    /**
     * ゴーグル側動作設定値格納用
     * ARSocketApplicationクラスのPropertyフィールドと同一インスタンス
     */
    private SettingProperty property;
    /**
     * 評価リスト格納用フィールド
     */
    private List<String> evaluationList = new ArrayList<>();
    /**
     * 言語リスト格納用フィールド
     */
    private List<String> languageList = new ArrayList<>();
    /**
     * 設定インポート用ファイルダイアログ表示用
     */
    private ActivityResultLauncher<Intent> openConfig;
    /**
     * 設定エクスポート用ファイルダイアログ表示用
     */
    private ActivityResultLauncher<Intent> saveConfig;
    /**
     * 言語追加時の音声ファイル追加用ファイルダイアログ表示用
     */
    private ActivityResultLauncher<Intent> addLanguage;

    /**
     * 評価データアップロード時の進捗表示ダイアログ
     */
    ProgressDialog uploadingEvaluationDataProgressDialog;
    /**
     * 言語追加時の音声ファイルアップロード進捗表示ダイアログ
     */
    ProgressDialog transferringAudioFilesProgressDialog;

    /**
     * エクスポート用設定データを記述したJSON文字列
     */
    private String exportedConfig = "";

    // UIパーツ
    /**
     * 評価リスト表示用スピナー
     */
    private Spinner spinnerEvaluationList;
    /**
     * 言語リスト表示用スピナー
     */
    private Spinner spinnerLanguages;
    /**
     * ログアウトボタン
     */
    private Button buttonLogout;

    // Webインタフェースへのログイン・アップロード関連
    /**
     * Webインタフェースへのログイン用認証トークン
     */
    private String authToken;
    /**
     * Webインタフェースへのログイン状態
     */
    private boolean loggedIn = false;
    /**
     * Webへのアップロード済み評価ファイルリスト
     */
    private List<String> uploadedList = new ArrayList<>();
    /**
     * WebインタフェースのベースURL
     */
    private final String API_BASE_URL = "http://humanitude-holoweb-env.eba-s9brepmg.ap-northeast-1.elasticbeanstalk.com/";

    /**
     * 認証トークン保存用SharedPreferences
     */
    SharedPreferences sharedPrefs;

    /**
     * アクティビティ初期化メソッド
     * 各UIの動作設定も記述
     * @param savedInstanceState 前のインスタンスの状態
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_setting);

        property = ((ARSocketApplication)getApplication()).Property;

        loggedIn = false;
        buttonLogout = findViewById(R.id.buttonLogout);
        buttonLogout.setEnabled(false);

        // Webインタフェースへのログイン
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        authToken = sharedPrefs.getString("auth_token", "");
        if(authToken.length() == 0)
            askLogin();
        else
            getEvaluationList(true);

        // 戻るボタンの処理（MainActivityへ戻る）
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(view -> finish());

        // 評価リストの表示・更新時の動作
        spinnerEvaluationList = findViewById(R.id.spinnerEvaluations);
        property.setEvaluationListListener(new_list -> {
            if(loggedIn) {
                evaluationList.clear();
                // ゴーグル側から送られてきた評価リストのうちアップロード済みの物を除いてスピナーに追加
                for (String evaluation_label : new_list) {
                    boolean already_uploaded = false;
                    for (int j = 0; j < uploadedList.size(); j++) {
                        if (evaluation_label.equals("evaluation_" + uploadedList.get(j) + ".json")) {
                            already_uploaded = true;
                            break;
                        }
                    }
                    if (!already_uploaded)
                        evaluationList.add(evaluation_label);
                }
                ConfigSettingActivity.this.runOnUiThread(() -> {
                    ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, evaluationList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerEvaluationList.setAdapter(adapter);
                });
            }
        });

        // アップロード用進捗ダイアログの初期化
        uploadingEvaluationDataProgressDialog = new ProgressDialog(this);
        uploadingEvaluationDataProgressDialog.setIndeterminate(false);
        uploadingEvaluationDataProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        uploadingEvaluationDataProgressDialog.setCanceledOnTouchOutside(false);
        uploadingEvaluationDataProgressDialog.setCancelable(false);

        // アップロードボタンの処理、評価データ受信時からのアップロード処理
        Button buttonUpload = findViewById(R.id.buttonUploadData);
        buttonUpload.setOnClickListener(view -> {
            if(!loggedIn)
                askLogin();
            else if(spinnerEvaluationList.getSelectedItem() != null){
                String selected = spinnerEvaluationList.getSelectedItem().toString();
                property.setRequestEvaluation(selected);
                Toast.makeText(ConfigSettingActivity.this, R.string.messageGettingEvaluationFile, Toast.LENGTH_LONG).show();
            }
        });
        property.setEvaluationDataListener(new_value -> {
            ConfigSettingActivity.this.runOnUiThread(() -> {
                String selected = spinnerEvaluationList.getSelectedItem().toString();
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.titleUploadEvaluationFile);
                builder.setMessage(getResources().getString(R.string.textUploadEvaluationFile) + ":" + selected);
                builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                    if(loggedIn) {
                        uploadingEvaluationDataProgressDialog.setMessage(getResources().getString(R.string.textUploadingEvaluationFile) + ":" + selected);
                        uploadingEvaluationDataProgressDialog.show();
                        requestPostJsonAPI("api/evaluation", new_value, authToken, (responseCode, responseBody) -> {
                            uploadingEvaluationDataProgressDialog.dismiss();
                            if (responseCode != 201) {
                                Toast.makeText(ConfigSettingActivity.this, getResources().getString(R.string.messageUploadFailed) + ":" + selected, Toast.LENGTH_LONG).show();
                                return;
                            }

                            JSONObject resJson = null;
                            boolean success = false;
                            try {
                                resJson = new JSONObject(responseBody);
                                if (resJson.has("message")) {
                                    String message = resJson.getString("message");
                                    if (message.equals("Created"))
                                        success = true;
                                }
                            } catch (JSONException e) {
                            }

                            if (success) {
                                Toast.makeText(ConfigSettingActivity.this, getResources().getString(R.string.messageUploadSuccessfully) + ":" + selected, Toast.LENGTH_LONG).show();
                                getEvaluationList(false);
                            } else
                                Toast.makeText(ConfigSettingActivity.this, getResources().getString(R.string.messageUploadFailed) + ":" + selected, Toast.LENGTH_LONG).show();
                        });
                    }
                });
                builder.setNegativeButton(R.string.no, (dialogInterface, i) -> {});
                builder.show();
            });
        });

        // ログアウトボタンの処理
        buttonLogout.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.titleLogoutDialog);
            builder.setMessage(R.string.textLogoutDialog);
            builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                sharedPrefs.edit()
                        .remove("auth_token")
                        .apply();
                loggedIn = false;
                buttonLogout.setEnabled(false);
            });
            builder.setNegativeButton(R.string.no, (dialogInterface, i) -> {});
            builder.show();
        });

        // 設定インポート時の処理
        Intent readConfigIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        readConfigIntent.addCategory(Intent.CATEGORY_OPENABLE);
        readConfigIntent.setType("application/json");
        openConfig = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if(result.getResultCode() == Activity.RESULT_OK) {
                String json = readTextFromUri(result.getData().getData());
                if(json != null && json.length() > 0) {
                    property.importConfig(json);
                }
            }
        });
        Button buttonImport = findViewById(R.id.buttonImportSettings);
        buttonImport.setOnClickListener(view -> {
            openConfig.launch(readConfigIntent);
        });
        property.setImportedConfigListener(new_value -> {
            ConfigSettingActivity.this.runOnUiThread(() -> {
                Toast.makeText(this, R.string.messageImportConfig, Toast.LENGTH_LONG).show();
            });
        });

        // 設定エクスポート時の処理
        Intent writeConfigIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        writeConfigIntent.addCategory(Intent.CATEGORY_OPENABLE);
        writeConfigIntent.setType("application/json");
        writeConfigIntent.putExtra(Intent.EXTRA_TITLE, "config.json");
        saveConfig = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if(result.getResultCode() == Activity.RESULT_OK) {
                Intent resultData = result.getData();
                if(resultData != null) {
                    Uri uri = resultData.getData();

                    try {
                        OutputStream os = getContentResolver().openOutputStream(uri);
                        os.write(exportedConfig.getBytes(StandardCharsets.UTF_8));

                        Toast.makeText(this, getResources().getText(R.string.messageExportConfig) + uri.toString(), Toast.LENGTH_LONG).show();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        Button buttonExport = findViewById(R.id.buttonExportSettings);
        buttonExport.setOnClickListener(view -> {
            property.requestExportConfig();
        });
        property.setExportConfigListener(new_value -> {
            exportedConfig = new_value;
            saveConfig.launch(writeConfigIntent);
        });

        // 顔モデルリストの表示、変更時の処理
        Spinner spinnerFaces = findViewById(R.id.spinnerFaces);
        AdapterView.OnItemSelectedListener faceListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Spinner spinner = (Spinner)adapterView;
                int face = spinner.getSelectedItemPosition();
                property.setFaceNumber(face, true, false);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        };
        spinnerFaces.setSelection(property.getFaceNumber());
        spinnerFaces.setOnItemSelectedListener(faceListener);
        property.setFaceListener(new_value -> {
            if(new_value >= 0 && new_value < spinnerFaces.getCount()) {
                ConfigSettingActivity.this.runOnUiThread(() -> {
                    spinnerFaces.setOnItemSelectedListener(null);
                    spinnerFaces.setSelection(new_value, false);
                    spinnerFaces.setOnItemSelectedListener(faceListener);
                });
            }
        });

        // 言語リストの表示、変更処理
        spinnerLanguages = findViewById(R.id.spinnerLanguages);
        languageList = new ArrayList<String>(property.getLanguageList());
        if(languageList.size() < 2)
            languageList = Arrays.asList(getResources().getStringArray(R.array.languages));
        initLanguageSpinner();
        int language = property.getLanguageNumber();
        if(language >= 0 && language < languageList.size())
            spinnerLanguages.setSelection(language);
        AdapterView.OnItemSelectedListener languageListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Spinner spinner = (Spinner) adapterView;
                int index = spinner.getSelectedItemPosition();
                if(index >= 0 && index < languageList.size())
                    property.setLanguageNumber(index, true, false);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        };
        spinnerLanguages.setOnItemSelectedListener(languageListener);
        property.setLanguageListener(new_value -> {
            if(new_value >= 0 && new_value < languageList.size()) {
                ConfigSettingActivity.this.runOnUiThread(() -> {
                    spinnerLanguages.setOnItemSelectedListener(null);
                    spinnerLanguages.setSelection(new_value, false);
                    spinnerLanguages.setOnItemSelectedListener(languageListener);
                });
            }
        });
        property.setLanguageListListener(new_list -> {
            boolean changed = false;
            if(languageList.size() != new_list.length)
                changed = true;
            else {
                for(int i = 0; i < languageList.size(); i++) {
                    if(!languageList.get(i).equals(new_list[i])) {
                        changed = true;
                        break;
                    }
                }
            }

            if(changed) {
                languageList = Arrays.asList(new_list);
                ConfigSettingActivity.this.runOnUiThread(() -> {
                    int index = property.getLanguageNumber();
                    initLanguageSpinner();
                    if (index >= 0 && index < languageList.size())
                        spinnerLanguages.setSelection(index);
                });
            }
        });

        // 音声ファイルアップロード進捗ダイアログの初期化
        transferringAudioFilesProgressDialog = new ProgressDialog(this);
        transferringAudioFilesProgressDialog.setMax(100);
        transferringAudioFilesProgressDialog.setProgress(0);
        transferringAudioFilesProgressDialog.setIndeterminate(false);
        transferringAudioFilesProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        transferringAudioFilesProgressDialog.setCanceledOnTouchOutside(false);
        transferringAudioFilesProgressDialog.setCancelable(false);

        // 言語追加時の処理
        Intent readAddLanguageIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        readAddLanguageIntent.addCategory(Intent.CATEGORY_OPENABLE);
        readAddLanguageIntent.setType("*/*");//
        String[] mimetypes = { "application/json", "audio/*" };
        readAddLanguageIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        readAddLanguageIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        addLanguage = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();

                Uri jsonUri = null;
                Map<String, SendFileInfo> audiofiles = new HashMap<>();

                Uri uri = data.getData();
                if (uri != null) {
                    SendFileInfo finfo = getFileInfoFromUri(uri);
                    String ext = MimeTypeMap.getFileExtensionFromUrl(finfo.fileName).toLowerCase(Locale.getDefault());
                    if (ext.equals("json"))
                        jsonUri = uri;
                    else if (ext.equals("wav") || ext.equals("mp3") || ext.equals("ogg"))
                        audiofiles.put(finfo.fileName, finfo);
                } else {
                    ClipData clip = data.getClipData();
                    int clipcount = clip.getItemCount();
                    for (int i = 0; i < clipcount; i++) {
                        ClipData.Item item = clip.getItemAt(i);
                        Uri itemUri = (item != null) ? item.getUri() : null;
                        if (itemUri != null) {
                            SendFileInfo finfo = getFileInfoFromUri(itemUri);
                            String ext = MimeTypeMap.getFileExtensionFromUrl(finfo.fileName).toLowerCase(Locale.getDefault());
                            if (ext.equals("json"))
                                jsonUri = itemUri;
                            else if (ext.equals("wav") || ext.equals("mp3") || ext.equals("ogg"))
                                audiofiles.put(finfo.fileName, finfo);
                        }
                    }
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.titleAddLanguageDialog);
                if (jsonUri != null) {
                    String json = readTextFromUri(jsonUri);
                    if (json != null && json.length() > 0) {
                        List<String> upload_files = new ArrayList<>();
                        List<String> notfound_files = new ArrayList<>();
                        try {
                            JSONObject lang_json = new JSONObject(json);

                            String[] scenarios = {"scenario1", "scenario2", "scenario3", "scenario4", "scenario2side", "scenario3side"};
                            for(int i = 0; i < scenarios.length; i++) {
                                JSONArray scenario = (JSONArray)lang_json.getJSONArray(scenarios[i]);
                                for(int j = 0; j < scenario.length(); j++) {
                                    JSONObject scenario_state = (JSONObject) scenario.get(j);
                                    separateFilesByExistenceInList(upload_files, notfound_files, scenario_state.getString("voice"), audiofiles);
                                }
                            }

                            separateFilesByExistenceInList(upload_files, notfound_files, lang_json.getString("warningMessage"), audiofiles);
                            separateFilesByExistenceInList(upload_files, notfound_files, lang_json.getString("startEvaluationMessage"), audiofiles);
                            separateFilesByExistenceInList(upload_files, notfound_files, lang_json.getString("endEvaluationMessage"), audiofiles);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if (notfound_files.size() > 0) {
                            String notfoundfiles = "";
                            for (int i = 0; i < notfound_files.size(); i++) {
                                if (notfoundfiles.length() > 0)
                                    notfoundfiles = notfoundfiles + ", ";
                                notfoundfiles = notfoundfiles + notfound_files.get(i);
                            }
                            builder.setMessage(notfoundfiles + " " + getResources().getString(R.string.messageAddLanguageNotFoundFiles));
                            builder.setNeutralButton(R.string.ok, (dialogInterface, i) -> {
                            });
                        } else {
                            builder.setMessage(upload_files.size() + " " + getResources().getString(R.string.messageAddLanguageConfirm));
                            builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                                property.addLanguage(json, audiofiles);
                                transferringAudioFilesProgressDialog.show();
                                transferringAudioFilesProgressDialog.setProgress(0);
                                transferringAudioFilesProgressDialog.setMessage("0% Start uploading...");
                            });
                            builder.setNegativeButton(R.string.no, ((dialogInterface, i) -> {
                            }));
                        }
                    }
                } else {
                    builder.setMessage(R.string.messageAddLanguageNotFoundRequiredFiles);
                    builder.setNeutralButton(R.string.ok, (dialogInterface, i) -> {});
                }

                builder.create();
                builder.show();
            }
        });
        Button buttonAddLanguage = findViewById(R.id.buttonAddLanguage);
        buttonAddLanguage.setOnClickListener(view -> {
            addLanguage.launch(readAddLanguageIntent);
        });

        // 言語削除時の処理
        Button buttonRemoveLanguage = findViewById(R.id.buttonRemoveLanguage);
        buttonRemoveLanguage.setOnClickListener(view -> {
            int selected_language = spinnerLanguages.getSelectedItemPosition();

            if(selected_language < 2)
                Toast.makeText(this, R.string.messageRemoveLanguageUnable, Toast.LENGTH_LONG).show();
            else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.titleRemoveLanguageDialog);
                builder.setMessage(getResources().getString(R.string.textRemoveLanguage) + spinnerLanguages.getSelectedItem().toString());
                builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                    property.removeLanguage(selected_language);
                });
                builder.setNegativeButton(R.string.no, (dialogInterface, i) -> {});
                builder.show();
            }
        });
        property.setRemovedLanguageListener(new_value -> {
            ConfigSettingActivity.this.runOnUiThread(() -> {
                Toast.makeText(this, R.string.messageRemoveLanguageSuccess, Toast.LENGTH_LONG).show();
            });
        });

        // 言語追加時の音声ファイルアップロード進捗状況更新の処理
        property.setUploadLanguageAudioFilesListener((filename, completed_filecount, completed_size, all_filecount, all_size) -> {
            ConfigSettingActivity.this.runOnUiThread(() -> {
                if(!transferringAudioFilesProgressDialog.isShowing())
                    transferringAudioFilesProgressDialog.show();

                int progress = (int)(completed_size * 100 / all_size);
                transferringAudioFilesProgressDialog.setProgress(progress);
                transferringAudioFilesProgressDialog.setMessage(progress + "%  " + completed_filecount + "/" + all_filecount);
            });
        });

        // 言語追加完了時の処理
        property.setCompletedAddLanguageListener(new_value -> {
            ConfigSettingActivity.this.runOnUiThread(() -> {
                transferringAudioFilesProgressDialog.dismiss();
                Toast.makeText(this, R.string.messageAddLanguageCompleted, Toast.LENGTH_LONG).show();
            });
        });

        // ビープモード切り替えの処理
        ToggleButton buttonBeep = findViewById(R.id.buttonBeep);
        buttonBeep.setChecked(property.getBeepMode());
        buttonBeep.setOnClickListener(view -> {
            boolean newBeepMode = property.toggleBeepMode(true, false);
            buttonBeep.setChecked(newBeepMode);
        });
        property.setBeepModeListener(new_value -> {
            ConfigSettingActivity.this.runOnUiThread(() -> buttonBeep.setChecked(new_value));
        });

        // 難易度レベル切り替えの処理
        Spinner spinnerLevel = findViewById(R.id.spinnerLevels);
        AdapterView.OnItemSelectedListener levelListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Spinner spinner = (Spinner) adapterView;
                property.setLevel(spinner.getSelectedItemPosition(), true, false);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        };
        spinnerLevel.setSelection(property.getLevel());
        spinnerLevel.setOnItemSelectedListener(levelListener);
        property.setLevelListener(new_value -> {
            if(new_value >= 0 && new_value < 3) {
                ConfigSettingActivity.this.runOnUiThread(() -> {
                    spinnerLevel.setOnItemSelectedListener(null);
                    spinnerLevel.setSelection(new_value, false);
                    spinnerLevel.setOnItemSelectedListener(levelListener);
                });
            }
        });

        // OpenAIキーの設定処理
        EditText editOpenAIAPIKey = findViewById(R.id.editTextOpenAIKey);
        editOpenAIAPIKey.setText(property.getOpenAIKey());
        Button buttonUpdateOpenAIKey = findViewById(R.id.buttonUpdateOpenAIKey);
        buttonUpdateOpenAIKey.setOnClickListener(view -> {
            property.setOpenAIKey(editOpenAIAPIKey.getText().toString(), true, false);
        });
        // APIキー類（OpenAI/Azure）のHololens側からの変更については現状無いであろうことから
        // (SettingPropertyで定義はしたが)変更リスナーの登録は行わない（変更の即時反映は必要ない、本アクティビティを開いたときにのみ設定すれば事足りる）。

        // Azure設定（リージョン、サブスクリプションキー）の処理
        EditText editAzureRegion = findViewById(R.id.editTextAzureRegion);
        editAzureRegion.setText(property.getAzureRegion());

        EditText editAzureKey = findViewById(R.id.editTextAzureSubscriptionKey);
        editAzureKey.setText(property.getAzureSubscriptionKey());

        Button buttonUpdateAzureSettings = findViewById(R.id.buttonUpdateAzureSettings);
        buttonUpdateAzureSettings.setOnClickListener(view -> {
            property.setAzureSettings(editAzureRegion.getText().toString(), editAzureKey.getText().toString(), true, false);
        });
    }

    /**
     * ファイル情報（ファイル名、ファイルサイズ）を取得する
     * @param uri ファイルの保存場所を表すURI
     * @return ファイル情報（ファイル名、ファイルサイズ）を格納したSendFileInfo型のインスタンス
     */
    private SendFileInfo getFileInfoFromUri(Uri uri) {
        SendFileInfo file = null;

        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);

        if(cursor != null) {
            if(cursor.moveToFirst()) {
                file = new SendFileInfo();
                file.fileName = cursor.getString(nameIndex);
                file.sizeInBytes = cursor.getLong(sizeIndex);
                file.uri = uri;
            }
        }

        return file;
    }

    /**
     * 特定のファイル名がlist中にが含まれているかで振り分けを行う。
     * 対象のfilenameが既にlistFoundもしくはlistNotFoundに含まれている場合は振り分けをしない。
     * 必要なファイルがlist中に全て含まれているかどうかをチェックするために用いる。
     * @param listFound list中にfilenameが含まれている場合、こちらのリストにfilenameが追加される
     * @param listNotFound list中にfilenameが含まれなかった場合、こちらのリストにfilenameが追加される
     * @param filename list中にキーとして含まれるかどうか判定する対象となるファイル名
     * @param list 特定のファイル名がキーとして含まれるかどうか判定の対象となるマップ
     */
    private void separateFilesByExistenceInList(List<String> listFound, List<String> listNotFound, String filename, Map<String, SendFileInfo> list) {
        if(listFound.contains(filename))
            return;
        else if(listNotFound.contains(filename))
            return;

        if(list.containsKey(filename))
            listFound.add(filename);
        else
            listNotFound.add(filename);
    }

    /**
     * 指定したURIで表されるファイルから文字列を読みだす
     * @param uri 読みだす対象となるファイルの位置を表すURI
     * @return 読み込まれた文字列
     */
    private String readTextFromUri(Uri uri) {
        String text = "";

        if(uri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] byteBuffer = new byte[8192];
                int length;
                int offset = 0;
                while ((length = inputStream.read(byteBuffer)) != -1) {
                    byteArrayOutputStream.write(byteBuffer, offset, length);
                    offset += length;
                }

                text = byteArrayOutputStream.toString("UTF-8");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return text;
    }

    /**
     * 言語選択スピナーを初期化する
     */
    private void initLanguageSpinner() {
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, languageList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguages.setAdapter(adapter);
    }

    /**
     * ログインがされていない状態であった場合、ログインを促すダイアログを表示し、
     * 承諾した場合に続くログイン処理を行う
     */
    private void askLogin() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.titleAskLoginDialog);
        builder.setMessage(R.string.textAskLoginDialog);

        builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> {
            LoginDialogFragment loginDialog = new LoginDialogFragment();
            loginDialog.setLoginListener((username, password) -> {
                int invalidParamMessage = R.string.messageUsernameAndPasswordEmpty;
                if(username.length() == 0) {
                    if(password.length() == 0)
                        invalidParamMessage = R.string.messageUsernameAndPasswordEmpty;
                    else
                        invalidParamMessage = R.string.messageUsernameEmpty;
                } else if(password.length() == 0)
                    invalidParamMessage = R.string.messagePasswordEmpty;
                else {
                    JSONObject json = new JSONObject();
                    try {
                        json.put("username", username);
                        json.put("password", password);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    requestPostJsonAPI("api/login", json.toString(), "", (RequestAPIResponseProcessor) (responseCode, responseBody) -> {
                        if(responseCode != 200 && responseCode != 400 && responseCode != 401 && responseCode != 404) {
                            Toast.makeText(this, R.string.messageNetworkError, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        try {
                            JSONObject resjson = new JSONObject(responseBody);

                            if(responseCode == 200 && resjson.has("token")) {
                                String atoken = resjson.getString("token");
                                if(atoken.length() > 0) {
                                    authToken = atoken;
                                    sharedPrefs.edit()
                                            .putString("auth_token", authToken)
                                            .apply();
                                    Toast.makeText(this, R.string.messageLoginSuccessfully, Toast.LENGTH_LONG).show();
                                    getEvaluationList(false);
                                } else
                                    Toast.makeText(this, R.string.messageLoginFailed, Toast.LENGTH_LONG).show();
                            } else
                                Toast.makeText(this, R.string.messageLoginFailed, Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {

                        }
                    });
                    return;
                }

                Toast.makeText(this, invalidParamMessage, Toast.LENGTH_LONG).show();
            });
            loginDialog.show(getSupportFragmentManager(), "Login");
        });
        builder.setNegativeButton(R.string.no, ((dialogInterface, i) -> {}));

        builder.create();
        builder.show();
    }

    /**
     * Webインタフェースへアップロード済み評価リストの取得を行う
     * @param ask_login ログインされていない場合、trueであればログインを促す。falseであればエラーを表すToastを表示して処理を終える。
     */
    private void getEvaluationList(boolean ask_login) {
        requestGetAPI("api/evaluations", authToken, (responseCode, responseBody) -> {
            if(responseCode != 200) {
                if(ask_login)
                    askLogin();
                else {
                    loggedIn = false;
                    buttonLogout.setEnabled(false);
                    Toast.makeText(this, R.string.messageNetworkError, Toast.LENGTH_SHORT).show();
                }
                return;
            }

            try {
                JSONObject resJson = new JSONObject(responseBody);
                if (resJson.has("evaluations")) {
                    uploadedList.clear();
                    JSONArray evaluations = resJson.getJSONArray("evaluations");
                    for (int i = 0; i < evaluations.length(); i++) {
                        JSONObject evaluation = evaluations.getJSONObject(i);
                        if (evaluation.has("evaluation_timestamp")) {
                            String str = evaluation.getString("evaluation_timestamp");
                            String[] digits = str.split("[-:T.]");
                            if (digits.length >= 6)
                                uploadedList.add(digits[0] + digits[1] + digits[2] + "_" + digits[3] + digits[4] + digits[5]);
                        }
                    }
                    property.setRequestEvaluationList();
                    loggedIn = true;
                    buttonLogout.setEnabled(true);
                } else {
                    sharedPrefs.edit().remove("auth_token").apply();
                    if(ask_login)
                        askLogin();
                    else {
                        loggedIn = false;
                        buttonLogout.setEnabled(false);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * WebインタフェースへJSONをPOSTするリクエストを追加する
     * @param api_path POSTするWebインタフェースのAPIのURI
     * @param send_json POSTするJSON文字列
     * @param auth_token 認証トークン
     * @param post_process POST完了時の処理を行うリスナー
     */
    private void requestPostJsonAPI(String api_path, String send_json, String auth_token, RequestAPIResponseProcessor post_process) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(new RequestAPIRunnable(api_path, true, send_json, auth_token, post_process));
    }

    /**
     * WebインタフェースへGETするリクエストを追加する
     * @param api_path GETするWebインタフェースのAPIのURI
     * @param auth_token 認証トークン
     * @param post_process GET完了時の処理を行うリスナー
     */
    private void requestGetAPI(String api_path, String auth_token, RequestAPIResponseProcessor post_process) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(new RequestAPIRunnable(api_path, false, null, auth_token, post_process));
    }

    /**
     * WebインタフェースのAPIに対してPOST/GETを行った際の完了時の処理をするためのリスナー用インタフェース
     */
    private interface RequestAPIResponseProcessor {
        /**
         * POST/GETが完了しレスポンスを受け取った際に呼び出すメソッド
         * @param responseCode 受信コード
         * @param responseBody 受信データのBody
         */
        void onReceivedResponse(int responseCode, String responseBody);
    }

    /**
     * WebインタフェースのAPIへのリクエスト処理を別スレッドで行うためのクラス
     * このインスタンスをExecutorServiceに渡して処理を行う
     */
    private class RequestAPIRunnable implements Runnable {
        /**
         * アクセスするAPIのURI
         */
        private String apiUrl;
        /**
         * (POST)送信するJSON文字列
         */
        private String sendJson;
        /**
         * 認証トークンの文字列
         */
        private String authToken;
        /**
         * POSTであればtrue/GETならばfalse
         */
        private boolean post = true;
        /**
         * リクエスト完了時（レスポンス受信時）の処理を行うリスナー
         */
        private RequestAPIResponseProcessor responseProcessor;
        /**
         * レスポンス処理リスナーをメインスレッドで動かすためのHandler
         */
        private Handler handler = new Handler(Looper.getMainLooper());

        /**
         * コンストラクタ。GET/POSTするためのパラメータを設定する。
         * @param api_path アクセスするWebインタフェースのAPIのURI
         * @param post POSTであればtrue, GETであればfalse
         * @param send_json POSTの場合でJSONを送信する場合に設定するJSON文字列
         * @param auth_token 認証トークン
         * @param response_processor 処理完了時（レスポンス受信時）の処理を表す
         */
        public RequestAPIRunnable(String api_path, boolean post, String send_json, String auth_token, RequestAPIResponseProcessor response_processor) {
            apiUrl = API_BASE_URL + api_path;
            this.post = post;
            sendJson = send_json;
            authToken = auth_token;
            responseProcessor = response_processor;
        }

        /**
         * APIリクエスト処理
         */
        @Override
        public void run() {
            String response = "";
            int responseCode = 0;

            try {
                HttpURLConnection con = null;
                URL url = new URL(apiUrl);

                con = (HttpURLConnection) url.openConnection();

                if(authToken != null && authToken.length() > 0)
                    con.setRequestProperty("Authorization", "Bearer " + authToken);

                if(post) {
                    con.setRequestMethod("POST");
                    con.setInstanceFollowRedirects(false);
                    con.setDoOutput(true);
                    con.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                    OutputStream os = con.getOutputStream();
                    PrintStream ps = new PrintStream(os);
                    ps.print(sendJson);
                    ps.close();
                } else {
                    con.setRequestMethod("GET");
                    con.setUseCaches(false);
                    con.setDoOutput(false);
                    con.setDoInput(true);

                    con.connect();
                }

                responseCode = con.getResponseCode();

                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
                String line;
                while ((line = reader.readLine()) != null) {
                    response += line;
                }
            } catch(MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            notifyResult(responseCode, response);
        }

        /**
         * APIリクエスト完了時の処理
         * @param responseCode 受信コード
         * @param responseBody 受信データ（BODY）
         */
        private void notifyResult(final int responseCode, final String responseBody) {
            handler.post((Runnable) () -> {
                if(responseProcessor != null)
                    responseProcessor.onReceivedResponse(responseCode, responseBody);
            });
        }
    }
}