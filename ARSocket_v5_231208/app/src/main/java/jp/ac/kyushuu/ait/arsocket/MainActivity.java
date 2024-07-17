package jp.ac.kyushuu.ait.arsocket;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.*;

/**
 * メインのアクティビティ用クラス
 * アプリの初期画面、ゴーグルとの接続、シナリオ等の設定、スコアの表示を行う
 */
public class MainActivity extends AppCompatActivity {
    // ゴーグル通信関連
    /**
     * ゴーグルとの通信スレッド用タスク
     */
    private AsyncTask communicationTask;
    /**
     * ゴーグルとの通信を行っているかを表すフラグ
     */
    private boolean communicating = false;

    /**
     * ゴーグル側動作設定値格納用
     * ARSocketApplicationクラスのPropertyフィールドと同一インスタンス
     */
    SettingProperty property;

    // UI関連
    /**
     * IPアドレス設定用スピナー
     * （Bluetooth接続版では使用しない）
     */
    private Spinner[] spinnerIP;
    /**
     * 警告音を再生する距離の閾値を設定するスピナー
     */
    private Spinner spinnerDistanceThreshold;   // Threshold
    /**
     * 接続可能なBluetoothデバイスを列挙、選択するスピナー
     */
    private Spinner spinnerBTDevices;
    /**
     * ゴーグルとの接続を実行するボタン
     */
    private Button buttonConnect;
    /**
     * 評価の開始・停止を行うボタン
     */
    private Button buttonEvaluation;
    /**
     * 顔の位置のリセットするモードのON/OFFを切り替えるボタン
     */
    private Button buttonReset;
    /**
     * シナリオモード/トレーニングモードの切り替え設定をするスピナー
     */
    private Spinner spinnerModes;
    /**
     * 現在のシナリオ中の進行ステートを表示するTextView
     */
    private TextView textCurrentState;
    /**
     * 各シナリオの選択を行うためのボタン
     */
    private Button[] buttonScenario;
    /**
     * マルチスコアを表示するTextView
     */
    private TextView textMultiScore;
    /**
     * 視線スコアを表示するTextView
     */
    private TextView textEyeScore;
    /**
     * 会話スコアを表示するTextView
     */
    private TextView textTalkScore;
    /**
     * 顔間距離を表示するTextView
     */
    private TextView textDistance;

    /**
     * ボタン等UIのデフォルトカラー（選択されていない時など）のID
     */
    private int defaultColorID = 0;
    /**
     * ボタン等UIのハイライトカラー（選択されているときなど）のID
     */
    private int highlightColorID = 0;

    /**
     * 警告音を鳴らすためのToneGenerator
     */
    private ToneGenerator toneGenerator;

    /**
     * 前に接続したIPアドレス, Bluetoothデバイスアドレス等を一時保存するためのPreference
     */
    private SharedPreferences sharedPref;

    /**
     * 分岐スレッドからメインスレッドで処理を実行するためのHandler
     */
    private Handler handler = new Handler();

    // Bluetooth接続関係
    /**
     * Bluetoothデバイス取得用のBluetoothAdapter
     */
    private BluetoothAdapter btAdapter;
    /**
     * 接続中（接続する対象に選択された）Bluetoothデバイス
     */
    private BluetoothDevice btDevice;
    /**
     * パーミッションのリクエストをした際にその結果をonRequestPermissionsResultで受け取る際の
     * レスポンスを特定するためのID
     */
    private static final int REQUEST_PERMISSION_BTCONNECT = 1001;

    /**
     * 以前に接続したBluetoothデバイスのアドレス
     */
    private String selectedBluetoothDeviceAddress;
    /**
     * 接続可能なBluetoothデバイスのリスト
     */
    private List<BluetoothDevice> btDeviceList = new ArrayList<BluetoothDevice>();

    // 評価時に利用するフィールド
    /**
     * デバッグ用文字列
     */
    private String messageForDebug;
    /**
     * 評価開始時の時間
     */
    private float initialTime = 0.0f;
    /**
     * 前フレームでの評価の状態
     */
    private boolean prevEvaluationState = false;

    /**
     * 評価時のスコアを表示するグラフ
     */
    private LineChart[] lineChart;

    /**
     * 1フレーム分のスコアを格納するクラス
     */
    private class Data {
        public float time;
        public float[] data = new float[3];
    }

    /**
     * 評価中の全フレームのスコアを格納するArrayList
     */
    private ArrayList<Data> scoreData = new ArrayList<>();

    // ログファイル出力関連
    /**
     * ログファイル書き出し用File
     */
    private File logFile = null;
    /**
     * パーミッションのリクエストをした際にその結果をonRequestPermissionsResultで受け取る際の
     * レスポンスを特定するためのID
     */
    private final int REQUEST_PERMISSION_WRITEFILE = 1000;
    /**
     * ログファイルに書き出し可能な状態を表すフラグ
     */
    private boolean recordingLog = false;

    // オーバーライドメソッド
    /**
     * アクティビティ初期化メソッド
     * 各値の初期化、各UIの動作の定義などを記述
     * @param savedInstanceState 前のインスタンスの状態
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        property = ((ARSocketApplication) getApplication()).Property;

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        spinnerBTDevices = (Spinner)findViewById(R.id.spinnerBtDevices);

        // IPアドレス設定 0 ～ 255 Start-----------------------------------
        int[] ipaddr = new int[4];
        ipaddr[0] = sharedPref.getInt("ip1", 192);
        ipaddr[1] = sharedPref.getInt("ip2", 168);
        ipaddr[2] = sharedPref.getInt("ip3", 0);
        ipaddr[3] = sharedPref.getInt("ip4", 1);
        selectedBluetoothDeviceAddress = sharedPref.getString("bt_device_address", "");

        spinnerIP = new Spinner[4];
        spinnerIP[0] = (Spinner) findViewById(R.id.spinner1);
        spinnerIP[1] = (Spinner) findViewById(R.id.spinner2);
        spinnerIP[2] = (Spinner) findViewById(R.id.spinner3);
        spinnerIP[3] = (Spinner) findViewById(R.id.spinner4);

        String items[] = new String[256];
        for (int ii = 0; ii < 256; ii++) {
            items[ii] = String.valueOf(ii);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items);
        //adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        for(int i = 0; i < spinnerIP.length; i++) {
            spinnerIP[i].setAdapter(adapter);

            // IPアドレス設定 0 ～ 255 End------------------------------------
            // IPアドレスの初期値設定 192.168.4.53
            spinnerIP[i].setSelection(ipaddr[i]);
        }

        // Threshold（距離閾値設定用のスピナー） -----------------------------------
        spinnerDistanceThreshold = (Spinner) findViewById(R.id.spinner5);
        spinnerDistanceThreshold.setSelection(2);

        // Android 6, API 23以上でファイル書き込みパーミッションの確認
        if (Build.VERSION.SDK_INT >= 23) {
            checkPermissionToWriteExternalStorage();
        } else {
            recordingLog = true;
            if (logFile == null) {
                openLogFile();
            }
        }

        // Message for display -------------------------
        textMultiScore = findViewById(R.id.multiscore_textView);
        textEyeScore = findViewById(R.id.eyescore_textView);
        textTalkScore = findViewById(R.id.talkscore_textView);
        textDistance = findViewById(R.id.distance_textView);

        defaultColorID = getResources().getColor(R.color.purple_200);
        highlightColorID = getResources().getColor(R.color.purple_500);

        // Graph ---------------------------------------
        lineChart = new LineChart[3];
        lineChart[0] = findViewById(R.id.line_chart_top);
        lineChart[1] = findViewById(R.id.line_chart_middle);
        lineChart[2] = findViewById(R.id.line_chart_bottom);

        for(int i = 0; i < lineChart.length; i++) {
            // enable touch gestures
            lineChart[i].setTouchEnabled(true);

            // enable scaling and dragging
            lineChart[i].setDragEnabled(true);
            lineChart[i].setScaleEnabled(true);
            lineChart[i].setDrawGridBackground(false);

            // if disabled, scaling can be done on x- and y-axis separately
            lineChart[i].setPinchZoom(true);

            // Grid背景色
            lineChart[i].setDrawGridBackground(true);

            // no description text
            lineChart[i].getDescription().setEnabled(true);

            lineChart[i].getDescription().setTextSize(16f);
        }

        lineChart[0].getDescription().setText("Multi modal");
        lineChart[1].getDescription().setText("Eye contact");
        lineChart[2].getDescription().setText("Talk");

        for(int i = 0; i < lineChart.length; i++) {
            // Grid縦軸を破線
            XAxis xAxis = lineChart[i].getXAxis();
            xAxis.enableGridDashedLine(10f, 10f, 0f);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setTextColor(Color.WHITE);

            YAxis leftAxis = lineChart[i].getAxisLeft();
            // Y軸最大最小設定
            leftAxis.setAxisMaximum(100f);
            leftAxis.setAxisMinimum(0f);
            // Grid横軸を破線
            leftAxis.enableGridDashedLine(10f, 10f, 0f);
            leftAxis.setDrawZeroLine(true);

            // 右側の目盛り
            lineChart[i].getAxisRight().setEnabled(false);

            // リアルタイム描画用に空のデータを設定
            LineData data = new LineData();
            lineChart[i].setData(data);
        }

        // Evaluation button -----------------------------------
        buttonEvaluation = findViewById(R.id.buttonEvaluation);
        if(property.getEvaluationMode()) {
            buttonEvaluation.setBackgroundColor(highlightColorID);
            buttonEvaluation.setText(R.string.buttonEvaluationStop);
        }
        buttonEvaluation.setOnClickListener(view -> {
            property.toggleEvaluationMode(true, true);
        });

        // リセットボタンの処理
        buttonReset = findViewById(R.id.buttonReset);
        buttonReset.setBackgroundColor(property.getResetMode() ? highlightColorID : defaultColorID);
        buttonReset.setOnClickListener(view -> {
            property.toggleResetMode(true, true);
        });

        // モードの処理
        spinnerModes = findViewById(R.id.spinnerModes);
        AdapterView.OnItemSelectedListener modeSpinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Spinner modeSpinner = (Spinner) adapterView;
                int index = modeSpinner.getSelectedItemPosition();
                boolean mode = property.setScenarioMode(index == 1, true, true);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        };
        spinnerModes.setOnItemSelectedListener(modeSpinnerListener);

        // ConfigSettingActivityへの遷移ボタンの処理
        ImageButton buttonConfig = findViewById(R.id.buttonConfig);
        buttonConfig.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, ConfigSettingActivity.class);
            startActivity(intent);
        });

        // シナリオの進行ステートの進む・戻る処理
        ImageButton buttonForward = findViewById(R.id.buttonForward);
        buttonForward.setOnClickListener(view -> {
            property.setScenarioStateForward();
        });
        ImageButton buttonBackward = findViewById(R.id.buttonBackward);
        buttonBackward.setOnClickListener(view -> {
            property.setScenarioStateBackward();
        });
        textCurrentState = findViewById(R.id.textCurrentState);

        // シナリオ選択ボタンの処理
        buttonScenario = new Button[4];
        buttonScenario[0] = findViewById(R.id.buttonScenario1);
        buttonScenario[1] = findViewById(R.id.buttonScenario2);
        buttonScenario[2] = findViewById(R.id.buttonScenario3);
        buttonScenario[3] = findViewById(R.id.buttonScenario4);
        for(int i = 0; i < buttonScenario.length; i++) {
            final int scenario_index = i;
            buttonScenario[i].setOnClickListener(view -> {
                property.setScenarioNumber(scenario_index, true, true);
            });
        }

        // ゴーグルからの設定変更反映を処理するリスナー
        // 評価モードの変更リスナー
        property.setEvaluationModeListener(new_value -> {
            handler.post(() -> {
                if(new_value) {
                    buttonEvaluation.setBackgroundColor(highlightColorID);
                    buttonEvaluation.setText(R.string.buttonEvaluationStop);
                    if (scoreData.size() != 0) {
                        initialTime = scoreData.get(scoreData.size() - 1).time;
                        Log.i("SET Socket ", Float.toString(initialTime));
                    }
                } else {
                    buttonEvaluation.setBackgroundColor(defaultColorID);
                    buttonEvaluation.setText(R.string.buttonEvaluation);
                }
            });
        });

        // リセットモードの変更リスナー
        property.setResetModeListener(new_value -> {
            handler.post(() -> buttonReset.setBackgroundColor(new_value ? highlightColorID : defaultColorID));
        });

        // シナリオ/トレーニングモードの変更リスナー
        property.setScenarioModeListener(new_value -> {
            invalidateOptionsMenu();
            handler.post(() -> {
                spinnerModes.setOnItemSelectedListener(null);
                spinnerModes.setSelection((new_value ? 1 : 0), false);
                setScenarioButtonsState(new_value ? property.getScenarioNumber(): -1);
                spinnerModes.setOnItemSelectedListener(modeSpinnerListener);
            });
        });

        // シナリオ番号ボタンの初期化、シナリオ番号変更リスナー
        setScenarioButtonsState(property.getScenarioMode() ? property.getScenarioNumber() : -1);
        property.setScenarioNumberListener(new_value -> {
            handler.post(() -> {
                setScenarioButtonsState(property.getScenarioMode() ? new_value : -1);
            });
        });

        // ゴーグルとの接続、通信の開始 ----------------------
        buttonConnect = findViewById(R.id.buttonConnect);
        buttonConnect.setOnClickListener(view -> {
            //String ip = "192.168.4.47";
            String ip = String.valueOf(spinnerIP[0].getSelectedItem()) + '.' +
                    String.valueOf(spinnerIP[1].getSelectedItem()) + '.' +
                    String.valueOf(spinnerIP[2].getSelectedItem()) + '.' +
                    String.valueOf(spinnerIP[3].getSelectedItem());

            int port = 14301;
            buttonConnect.setEnabled(false);

            if (!communicating) {
                //task = new TCPAsyncTask();
                property.setRequestEvaluationList();

                communicationTask = new BTAsyncTask(btAdapter, btDevice);
                communicationTask.execute((Object) ip, (Object) port, new Object());
                communicating = true;
            } else {
                communicationTask.cancel(true);
                communicating = false;
            }

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("ip1", Integer.parseInt(String.valueOf(spinnerIP[0].getSelectedItem())));
            editor.putInt("ip2", Integer.parseInt(String.valueOf(spinnerIP[1].getSelectedItem())));
            editor.putInt("ip3", Integer.parseInt(String.valueOf(spinnerIP[2].getSelectedItem())));
            editor.putInt("ip4", Integer.parseInt(String.valueOf(spinnerIP[3].getSelectedItem())));
            if(btDevice != null)
                editor.putString("bt_device_address", btDevice.getAddress());
            editor.apply();
        });

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null)
            Toast.makeText(this, "This device doesn't support BT.", Toast.LENGTH_SHORT).show();

        initializeBTDeviceSettings();

        // 警告音
        toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 50);
    }

    /**
     * オプションメニューの表示処理
     * @param menu 表示するメニューのインスタンス
     * @return 表示が行われたかどうか
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * オプションメニューの表示設定を行う
     * @param menu 表示するメニューのインスタンス
     * @return 表示が行われたかどうか
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem mode_setting = menu.findItem(R.id.mode_setting);
        if(property.getScenarioMode()) {
            mode_setting.setTitle(getResources().getString(R.string.buttonScenarioMode));
        } else {
            mode_setting.setTitle(getResources().getString(R.string.buttonTrainingMode));
        }
        return true;
    }

    /**
     * オプションメニューの項目が選択された際の処理
     * @param item 選択されたメニュー項目
     * @return デフォルト処理を行う場合はtrue、処理をこのメソッド内で完結させる場合はfalse
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch(id) {
            case R.id.menu_japanese:
                property.setLanguageNumber(0, true, false);
                break;
            case R.id.menu_english:
                property.setLanguageNumber(1, true, false);
                break;
            case R.id.menu_beepon:
                property.setBeepMode(true, true, false);
                break;
            case R.id.menu_beepoff:
                property.setBeepMode(false, true, false);
                break;
            case R.id.menu_training:
                property.setScenarioMode(false, true, false);
                break;
            case R.id.menu_scenario:
                property.setScenarioMode(true, true, false);
                break;
            case R.id.menu_easy:
                property.setLevel(0, true, false);
                break;
            case R.id.menu_medium:
                property.setLevel(1, true, false);
                break;
            case R.id.menu_hard:
                property.setLevel(2, true, false);
                break;
            }

        invalidateOptionsMenu();
        return super.onOptionsItemSelected(item);
    }

    /**
     * パーミッションのリクエストに対する返答結果を受け取る
     * @param requestCode The request code
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
     *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
     *
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_WRITEFILE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recordingLog = true;
                if (logFile == null) {
                    openLogFile();
                }
            } else {
                Toast toast = Toast.makeText(this,
                        "No permission to access the storage.", Toast.LENGTH_SHORT);
                toast.show();
            }
        } else if(requestCode == REQUEST_PERMISSION_BTCONNECT) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeBTDeviceSettings();
            } else {
                Toast.makeText(this, "No permission to connect BT.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // プライベートメソッド
    /**
     * シナリオ番号のボタン状態を更新する
     * 選択中のシナリオのボタンのみハイライト表示し、それ以外をデフォルトカラーにする
     * @param scenarioIndex 更新するシナリオのインデックス
     */
    private void setScenarioButtonsState(int scenarioIndex){
        int[] colors = {defaultColorID, defaultColorID, defaultColorID, defaultColorID};

        if(scenarioIndex >= 0 && scenarioIndex < 4)
            colors[scenarioIndex] = highlightColorID;

        for(int i = 0; i < buttonScenario.length; i++)
            buttonScenario[i].setBackgroundColor(colors[i]);
    }

    /**
     * Bluetoothデバイスの取得、選択・設定UIの初期化
     */
    private void initializeBTDeviceSettings() {
        btDevice = null;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_PERMISSION_BTCONNECT);
            return;
        }

        Set<BluetoothDevice> devices = btAdapter.getBondedDevices();
        String[] device_names = new String[devices.size() + 1];
        device_names[0] = getResources().getStringArray(R.array.bluetoothDevices)[0];
        int selected_device = -1;
        for(BluetoothDevice device : devices) {
            int index = btDeviceList.size();
            btDeviceList.add(device);
            device_names[index + 1] = device.getName();

            if(selectedBluetoothDeviceAddress.length() > 0 && selectedBluetoothDeviceAddress.equals(device.getAddress()))
                selected_device = index;
        }

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, device_names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBTDevices.setAdapter(adapter);

        buttonConnect.setBackgroundColor(defaultColorID);
        buttonConnect.setEnabled(false);

        spinnerBTDevices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Spinner spinner = (Spinner) adapterView;
                int index = spinner.getSelectedItemPosition();
                if(!communicating)
                    buttonConnect.setEnabled(index > 0);
                if(index > 0)
                    btDevice = btDeviceList.get(index - 1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        if(selected_device >= 0)
            spinnerBTDevices.setSelection(selected_device + 1);
    }

    /**
     * 送信するJSON文字列を生成するメソッド
     * @return
     */
    private String getJsonToSend() {
        String cmd = "";
        cmd = property.getRequestingJson();

        return cmd;
    }

    /**
     * 受信したJSON文字列を読んで処理を行うメソッド
     * @param msg 処理するJSON文字列
     */
    private void processReceivedJson(String msg) {
        try {
            JSONObject inputJson = new JSONObject(msg);

            if (!inputJson.isNull("CurrentTime")) {
                messageForDebug = msg;
                String currentMultiScore = String.valueOf(inputJson.getInt("MultiModalScore"));
                String currentEyeScore = String.valueOf(inputJson.getInt("EyeContactScore"));
                String currentTalkScore = String.valueOf(inputJson.getInt("VoiceScore"));
                String currentDistance = String.valueOf(inputJson.getInt("Distance"));

                boolean evaluating = false;
                if(inputJson.has("evaluation")) {
                    evaluating = inputJson.getBoolean("evaluation");
                    property.setEvaluationMode(evaluating, false, true);
                }

                if(inputJson.has("reset"))
                    property.setResetMode(inputJson.getBoolean("reset"), false, true);

                if(inputJson.has("mode"))
                    property.setScenarioMode(inputJson.getBoolean("mode"), false, true);

                int scenario_num = -1;
                if(inputJson.has("senarionum"))
                    scenario_num = inputJson.getInt("senarionum");
                property.setScenarioNumber(scenario_num, false, true);

                int scenarioState = -1;
                if(inputJson.has("scenarioState"))
                    scenarioState = inputJson.getInt("scenarioState");
                property.setScenarioState(scenarioState);
                handler.post(() -> {
                    int state = property.getScenarioState();
                    if(state < 0)
                        textCurrentState.setText(R.string.textStateNone);
                    else
                        textCurrentState.setText(Integer.toString(state));
                });

                if(inputJson.has("evaluationList")) {
                    JSONArray array = inputJson.getJSONArray("evaluationList");
                    String[] list = new String[array.length()];
                    for(int i = 0; i < array.length(); i++) {
                        String l = (String)array.get(i);
                        list[i] = l;
                    }
                    if(list.length > 0)
                        property.setEvaluationList(list);
                }

                if(inputJson.has("evaluationData")) {
                    JSONObject data = inputJson.getJSONObject("evaluationData");
                    if(data.has("results")) {
                        JSONArray results = data.getJSONArray("results");
                        if(results.length() > 0)
                            property.setEvaluationData(data.toString());
                    }
                }

                if(inputJson.has("exportConfig")) {
                    JSONObject export_config = inputJson.getJSONObject("exportConfig");
                    if(export_config.has("Date")) {
                        String date = export_config.getString("Date");
                        if(date != null && date.length() > 0)
                            property.exportConfig(export_config.toString());
                    }
                }

                if(inputJson.has("importedConfig")) {
                    boolean imported = inputJson.getBoolean("importedConfig");
                    if(imported)
                        property.importedConfig();
                }

                if(inputJson.has("face"))
                    property.setFaceNumber(inputJson.getInt("face"), false, true);

                if(inputJson.has("languageIndex"))
                    property.setLanguageNumber(inputJson.getInt("languageIndex"), false, true);
                else if(inputJson.has("language"))
                    property.setLanguageNumber(inputJson.getBoolean("language") ? 1 : 0, false, true);

                if(inputJson.has("languageList")) {
                    JSONArray array = inputJson.getJSONArray("languageList");
                    String[] list = new String[array.length()];
                    for(int i = 0; i < array.length(); i++) {
                        String l = (String)array.get(i);
                        list[i] = l;
                    }
                    property.setLanguageList(list);
                }

                if(inputJson.has("startAddLanguage")) {
                    if(inputJson.getBoolean("startAddLanguage")) {
                        property.startAddLanguage();
                        ((BTAsyncTask) communicationTask).setSendFileMode(true);
                    }
                }

                if(inputJson.has("removedLanguage")) {
                    if(inputJson.getBoolean(("removedLanguage"))) {
                        property.removedLanguage();
                    }
                }

                if(inputJson.has("beep"))
                    property.setBeepMode(inputJson.getBoolean("beep"), false, true);

                if(inputJson.has("level"))
                    property.setLevel(inputJson.getInt("level"), false, true);

                if(inputJson.has("openAIAPIKey"))
                    property.setOpenAIKey(inputJson.getString("openAIAPIKey"), false, true);

                if(inputJson.has("azureRegion") || inputJson.has("azureSubscriptionKey")) {
                    String region = property.getAzureRegion();
                    String key = property.getAzureSubscriptionKey();
                    if(inputJson.has("azureRegion"))
                        region = inputJson.getString("azureRegion");
                    if(inputJson.has("azureSubscriptionKey"))
                        key = inputJson.getString("azureSubscriptionKey");
                    property.setAzureSettings(region, key, false, true);
                }


                handler.post(() -> {
                    textMultiScore.setText(currentMultiScore);
                    textEyeScore.setText(currentEyeScore);
                    textTalkScore.setText(currentTalkScore);
                    textDistance.setText(currentDistance);
                });

                if (evaluating && prevEvaluationState) {
                    Data _data = new Data();
                    _data.time = (float)inputJson.getDouble("TotalTime") + initialTime;
                    _data.data[0] = (float)inputJson.getDouble("MultiModalScore");
                    _data.data[1] = (float)inputJson.getDouble("EyeContactScore");
                    _data.data[2] = (float)inputJson.getDouble("VoiceScore");
                    Log.d("ARSocket.MainActivity", "time:" + _data.time + ", top:" + _data.data[0] + ", middle:" + _data.data[1] + ", bottom:" + _data.data[2]);
                    synchronized (scoreData) {
                        scoreData.add(_data);
                    }
                }

                prevEvaluationState = evaluating;

                // 振動を発生
                float Threshold = Float.parseFloat(String.valueOf(spinnerDistanceThreshold.getSelectedItem()));
                if ((float)inputJson.getDouble("Distance") > Threshold) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200); // 200 is duration in ms
                    else
                        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200); // 200 is duration in ms
                }

                if (recordingLog) {
                    try {
                        FileOutputStream out = new FileOutputStream(logFile, true);
                        out.write((msg + "\n").getBytes());
                        out.close();
                    } catch (FileNotFoundException fileNotEx) {
                        MainActivity.this.runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "File not found!", Toast.LENGTH_SHORT).show();    // "File not found!"と表示.
                        });
                    } catch (IOException ioEx) {    // IOエラー.
                        MainActivity.this.runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "IO error!", Toast.LENGTH_SHORT).show();    // "IO Error!"と表示.
                        });
                    }
                }
            }
        } catch (JSONException err) {
            System.out.println("Exception : "+err.toString());
        }
    }

    /**
     * グラフへのリアルタイム表示を行うためのスコアデータの追加
     */
    private void addScoreEntry() {
        if(scoreData.size() == 0)
            return;

        int x_range = 10;        // 幅の最大値

        for(int i = 0; i < lineChart.length; i++) {
            LineData data = lineChart[i].getData();

            //dataの中身が空の場合mChartからグラフデータを取得
            if (data != null) {
                //リアルタイムでデータを更新する場合はILineDataSetを使う
                //データをセットする際にインデックスを指定
                ILineDataSet set = data.getDataSetByIndex(0);

                //setの中身が空の場合dataに追加
                if (set == null) {
                    set = createLineDataSet(i);
                    data.addDataSet(set);
                }

                synchronized (scoreData) {
                    for (int j = data.getEntryCount(); j < scoreData.size(); j++) {
                        float t = scoreData.get(j).time;
                        float d = scoreData.get(j).data[i];
                        //data.addEntry(new Entry(set.getEntryCount(), d), 0);
                        data.addEntry(new Entry(t, d), 0);
                    }
                }

                //更新を通知
                data.notifyDataChanged();
                lineChart[i].notifyDataSetChanged();
                lineChart[i].setVisibleXRangeMaximum(x_range);
                //lineChart[i].moveViewToX(data.getEntryCount() - x_range - 1);
                lineChart[i].moveViewToX(data.getEntryCount());
            }
        }
    }

    /**
     * スコアのグラフ表示用のデータセットを初期化する
     * @param n 対象スコアのインデックス（0:マルチスコア、1:アイコンタクト、2:会話）を指定する
     * @return 初期化したデータセット
     */
    private LineDataSet createLineDataSet(int n) {

        LineDataSet set;// = new LineDataSet(null, "Distance_top");

        switch (n) {
            case 0:
                set = new LineDataSet(null, "Top");
                set.setColor(Color.RED);
                set.setCircleColor(Color.RED);
                break;
            case 1:
                set = new LineDataSet(null, "Middle");
                set.setColor(Color.GREEN);
                set.setCircleColor(Color.GREEN);
                break;
            case 2:
            default:
                set = new LineDataSet(null, "Bottom");
                set.setColor(Color.BLUE);
                set.setCircleColor(Color.BLUE);
                break;
        }
        set.setDrawIcons(false);
        set.setLineWidth(1f);
        set.setCircleRadius(3f);
        set.setDrawCircleHole(false);
        set.setValueTextSize(0f);
        set.setDrawFilled(false);
        set.setFormLineWidth(1f);
        set.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
        set.setFormSize(15.f);

        set.setFillColor(Color.BLUE);

        return set;
    }

    /**
     * 外部ストレージが書き込み可能かチェックする
     * @return 書き込み可能であればtrue、不可であればfalse
     */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /**
     * 外部ストレージが読み込み可能であるかをチェックする
     * @return 読み込み可能であればtrue、不可であればfalse
     */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    /**
     * 外部ストレージへの書き込み権限が許可されているかをチェックし、
     * 権限が無ければリクエストを行う
     */
    private void checkPermissionToWriteExternalStorage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE };
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_WRITEFILE);
        } else {
            recordingLog = true;
            if (logFile == null){
                openLogFile();
            }
        }
    }

    /**
     * 動作ログファイルを書き込みできるように初期化する
     */
    private void openLogFile() {
        try {
            android.content.Context context = getApplicationContext();

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
            Date now = new Date();
            String filename = formatter.format(now) + ".json";

            if (isExternalStorageWritable()) {
                // https://akira-watson.com/android/external-storage-file.html
                // https://qiita.com/kazhida/items/12ab5ce655e7c5a463ff
                File extDir = context.getExternalFilesDir(null);
                logFile = new File(extDir, filename);
                FileOutputStream out = new FileOutputStream(logFile);
                //out.write((now+"\n").getBytes());
                out.close();
                Toast.makeText(this, logFile.toString(), Toast.LENGTH_LONG).show();
            }

        } catch (FileNotFoundException fileNotEx) {
            Toast.makeText(this, "File not found!", Toast.LENGTH_SHORT).show();
        } catch (IOException ioEx) {
            Toast.makeText(this, "IO error!", Toast.LENGTH_SHORT).show();
        }
    }

    // ゴーグルとの通信を行うためのクラス
    /**
     * Bluetoothによるゴーグルとの通信処理を別スレッドで行うためのクラス
     */
    private class BTAsyncTask extends AsyncTask {
        BluetoothAdapter bluetoothAdapter;
        BluetoothDevice bluetoothDevice;

        private final UUID BT_UUID = UUID.fromString(
                "41eb5f39-6c3a-4067-8bb9-bad64e6e0908");
        BluetoothSocket bluetoothSocket;
        InputStream inputStream;
        OutputStream outputStream;
        byte[] incomingBuff = new byte[16777216];

        boolean sendFileMode = false;
        public void setSendFileMode(boolean mode) {
            sendFileMode = mode;
        }

        protected BTAsyncTask(BluetoothAdapter adapter, BluetoothDevice device){
            bluetoothAdapter = adapter;
            bluetoothDevice = device;
        }

        /**
         * 別スレッドで実行するゴーグルとの通信の待ち受け・返答処理を行うメソッド
         * @param objects メソッドに渡す引数。ここでは使わない。
         * @return 処理結果を表す文字列
         */
        @SuppressLint("MissingPermission")
        @Override
        protected Object doInBackground(Object[] objects) {
            if(bluetoothDevice == null)
                return "NoDevice";

            try {
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);

                try {
                    bluetoothSocket.connect();
                    MainActivity.this.runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, getString(R.string.messageConnected) + bluetoothDevice.getName(), Toast.LENGTH_LONG).show();

                        buttonConnect.setEnabled(true);
                        buttonConnect.setText(R.string.buttonDisconnect);
                        buttonConnect.setBackgroundColor(highlightColorID);
                    });
                } catch (Exception e) {
                    MainActivity.this.runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, getString(R.string.messageFailedToConnect) + bluetoothDevice.getName(), Toast.LENGTH_LONG).show();
                        communicating = false;
                        buttonConnect.setEnabled(true);
                        buttonConnect.setBackgroundColor(defaultColorID);
                    });
                    return (Object) e.toString();
                }

                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
                final int timeoutTime = 15000;
                boolean isTimeout = false;

                while (!isCancelled()) {

                    if(sendFileMode) {
                        Pair<String, SendFileInfo> pair = property.getAddLanguageAudioFile();

                        if(pair != null) {
                            byte[] fnamebuf = pair.first.getBytes();
                            byte[] fnamelen = ByteBuffer.allocate(Integer.BYTES).putInt(fnamebuf.length).array();
                            outputStream.write(fnamelen);
                            outputStream.write(fnamebuf);

                            InputStream inputStream = getContentResolver().openInputStream(pair.second.uri);
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            byte[] databuf = new byte[64000000];
                            int length;
                            int offset = 0;
                            while ((length = inputStream.read(databuf)) != -1) {
                                byteArrayOutputStream.write(databuf, offset, length);
                                offset += length;
                            }

                            byte[] datalen = ByteBuffer.allocate(Integer.BYTES).putInt(offset).array();
                            outputStream.write(datalen);
                            outputStream.write(databuf, 0, offset);

                            property.completedUploadAudioFile(pair.first);
                        } else {
                            int zerolen = 0;
                            byte[] zerobuf = ByteBuffer.allocate(Integer.BYTES).putInt(zerolen).array();
                            outputStream.write(zerobuf);
                            outputStream.write(zerobuf);
                        }

                        isTimeout = waitData(inputStream, timeoutTime);
                        if(isTimeout)
                            break;
                        int incomingBytes = inputStream.read(incomingBuff, 0, Integer.BYTES);

                        byte[] buff = new byte[incomingBytes];
                        System.arraycopy(incomingBuff, 0, buff, 0, incomingBytes);
                        int len = ByteBuffer.wrap(buff).getInt();

                        isTimeout = waitData(inputStream, timeoutTime);
                        if(isTimeout)
                            break;
                        incomingBytes = inputStream.read(incomingBuff, 0, len);

                        int offset = incomingBytes;
                        int restlen = len - offset;
                        while (restlen > 0) {
                            isTimeout = waitData(inputStream, timeoutTime);
                            if(isTimeout)
                                break;
                            int l = inputStream.read(incomingBuff, offset, restlen);
                            offset = offset + l;
                            restlen = len - offset;
                        }

                        if(isTimeout)
                            break;

                        if (len > 0) {
                            buff = new byte[len];
                            System.arraycopy(incomingBuff, 0, buff, 0, len);
                            String msg = new String(buff, StandardCharsets.UTF_8);

                            JSONObject resJson = new JSONObject(msg);
                            if(resJson.has("endFileReceived")) {
                                if(resJson.getBoolean("endFileReceived")) {
                                    setSendFileMode(false);
                                    property.addedLanguage();
                                }
                            }
                        }

                    } else {
                        String cmd = getJsonToSend();

                        byte[] cmdbuff = cmd.getBytes();
                        byte[] cmdlen = ByteBuffer.allocate(Integer.BYTES).putInt(cmdbuff.length).array();
                        outputStream.write(cmdlen);
                        outputStream.write(cmdbuff);

                        String msg;
                        //String msg = "{\"CurrentTime\":\"20220706_174452\",\"TotalTime\":0.5088662505149841,\"MultiModalTime\":0.0,\"VoiceTime\":0.5088662505149841,\"EyeContactTime\":0.0,\"GazeTime\":0.0,\"TouchTime\":0.0,\"VoiceScore\":100.0,\"EyeContactScore\":0.0,\"TouchScore\":0.0,\"GazeScore\":0.0,\"MultiModalScore\":0.0,\"TouchState\":-1,\"VoiceState\":1,\"EyeContactState\":0,\"GazeState\":0,\"Distance\":96.70360565185547,\"evaluation\":false,\"reset\":false,\"language\":false,\"beep\":false,\"mode\":false,\"forward\":false,\"backword\":false,\"senario\":false,\"senarionum\":0,\"level\":0}";

                        isTimeout = waitData(inputStream, timeoutTime);
                        if(isTimeout)
                            break;

                        int incomingBytes = inputStream.read(incomingBuff, 0, Integer.BYTES);
                        byte[] buff = new byte[incomingBytes];
                        System.arraycopy(incomingBuff, 0, buff, 0, incomingBytes);
                        int len = ByteBuffer.wrap(buff).getInt();

                        isTimeout = waitData(inputStream, timeoutTime);
                        if(isTimeout)
                            break;
                        incomingBytes = inputStream.read(incomingBuff, 0, len);
                        int offset = incomingBytes;
                        int restlen = len - offset;
                        while (restlen > 0) {
                            isTimeout = waitData(inputStream, timeoutTime);
                            if(isTimeout)
                                break;
                            int l = inputStream.read(incomingBuff, offset, restlen);
                            offset = offset + l;
                            restlen = len - offset;
                        }

                        if(isTimeout)
                            break;

                        if (len > 0) {
                            buff = new byte[len];
                            System.arraycopy(incomingBuff, 0, buff, 0, len);
                            msg = new String(buff, StandardCharsets.UTF_8);

                            processReceivedJson(msg);
                        }

                        if (isCancelled()) {
                            break;
                        }

                        // add data in real time
                        if (property.getEvaluationMode())
                            addScoreEntry();
                    }

                    Thread.sleep(200);
                }

                inputStream.close();
                outputStream.close();
                bluetoothSocket.close();

                communicating = false;

                if(isTimeout) {
                    handler.post(() -> {
                        Toast.makeText(MainActivity.this, R.string.messageTimeoutToConnect, Toast.LENGTH_LONG).show();
                    });
                }

                String str = "Connection terminated";
                messageForDebug = str;
                handler.post(() -> {
                    buttonConnect.setBackgroundColor(defaultColorID);
                    buttonConnect.setText(R.string.buttonConnect);
                });

                return (Object) str;
            } catch (IOException e) {
                return e.toString();
            } catch (JSONException e) {
                return e.toString();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return e.toString();
            }
        }

        @Override
        protected void onCancelled() {
            MainActivity.this.runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, R.string.messageCanceledToConnect, Toast.LENGTH_LONG).show();
                communicating = false;
                buttonConnect.setEnabled(true);
                buttonConnect.setText(R.string.buttonConnect);
                buttonConnect.setBackgroundColor(defaultColorID);
            });
        }

        private boolean waitData(InputStream input, int timeoutMilliSeconds) {
            int waitingTime = 0;
            try {
                while (input.available() <= 0) {
                    Thread.sleep(10);
                    waitingTime += 10;
                    if (waitingTime >= timeoutMilliSeconds)
                        return true;
                }
            } catch (IOException e) {
            } catch (InterruptedException e) {
            }

            return false;
        }
    }

    /**
     * TCP接続によるゴーグルとの通信処理を別スレッドで行うためのクラス
     * Bluetooth接続に対応したため現在は使用していないが、念のため残しています。
     * 新規追加された通信内容（言語の追加等）の処理については未対応
     */
    private class TCPAsyncTask extends AsyncTask {
        Socket socket = null;
        BufferedReader reader = null;
        PrintWriter writer = null;

        @Override
        protected Object doInBackground(Object... obj) {
            try {
                InetSocketAddress address = new InetSocketAddress((String) obj[0], (int) obj[1]);
                socket = new Socket();

                try {
                    socket.connect(address, 3000);
                    MainActivity.this.runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "接続しました。", Toast.LENGTH_LONG).show();
                    });
                    handler.post(() -> {
                        buttonConnect.setBackgroundColor(highlightColorID);
                    });
                } catch (Exception e) {
                    MainActivity.this.runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "接続に失敗しました。", Toast.LENGTH_LONG).show();
                        buttonConnect.setBackgroundColor(defaultColorID);
                    });
                    handler.post(() -> {
                        buttonConnect.setBackgroundColor(defaultColorID);
                    });
                    return (Object) e.toString();
                }

                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);

                while (!isCancelled()) {
                    String cmd = getJsonToSend();

                    writer.println(cmd);
                    //Log.i("Send ", cmd);

                    String msg;
                    //String msg = "{\"CurrentTime\":\"20220706_174452\",\"TotalTime\":0.5088662505149841,\"MultiModalTime\":0.0,\"VoiceTime\":0.5088662505149841,\"EyeContactTime\":0.0,\"GazeTime\":0.0,\"TouchTime\":0.0,\"VoiceScore\":100.0,\"EyeContactScore\":0.0,\"TouchScore\":0.0,\"GazeScore\":0.0,\"MultiModalScore\":0.0,\"TouchState\":-1,\"VoiceState\":1,\"EyeContactState\":0,\"GazeState\":0,\"Distance\":96.70360565185547,\"evaluation\":false,\"reset\":false,\"language\":false,\"beep\":false,\"mode\":false,\"forward\":false,\"backword\":false,\"senario\":false,\"senarionum\":0,\"level\":0}";

                    if ((msg = reader.readLine()) != null) {
                        processReceivedJson(msg);
                    }

                    if (isCancelled()) {
                        break;
                    }

                    // add data in real time
                    if(property.getEvaluationMode()) addScoreEntry();

                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                writer.close();
                reader.close();
                socket.close();

                String str = "Connection terminated";
                messageForDebug = str;
                handler.post(() -> {
                    buttonConnect.setBackgroundColor(defaultColorID);
                });

                return (Object) str;
            } catch (Exception e) {
                return (Object) e.toString();
            }
        }

        @Override
        protected void onPostExecute(Object obj) {
            //doInBackgroundの戻り値が引数に渡される
            //メインスレッドで実行されるため、画面周りの処理ができる。

            //画面にメッセージを表示する
            android.content.Context context = getApplicationContext();
            android.widget.Toast t = android.widget.Toast.makeText(context, (String) obj, android.widget.Toast.LENGTH_LONG);
            t.show();
        }

        @Override
        protected void onCancelled() {
            // TODO Auto-generated method stub
            MainActivity.this.runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "切断しました", Toast.LENGTH_LONG).show();
            });
        }
    }
}