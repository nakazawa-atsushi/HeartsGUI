package jp.ac.kyushuu.ait.arsocket;

import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * ゴーグル側MRアプリの各種設定項目の状態を管理するクラス
 */
public class SettingProperty {

    // 各種設定項目の設定状態を表す変数群
    /**
     * 評価モードのON/OFFを表す変数
     */
    private boolean evaluationMode = false;
    /**
     * リセットモードのON/OFFを表す変数
     */
    private boolean resetMode = false;
    /**
     * 動作モード(シナリオモード:true/トレーニングモード:false）を表す変数
     */
    private boolean scenarioMode = false; // true->Scenario
    /**
     * シナリオモードで選択中のシナリオを表す変数
     */
    private int scenarioNumber = 0;
    /**
     * 現在のシナリオ中の進行ステートを表す変数
     */
    private int currentState = -1;
    /**
     * インポート/エクスポートする設定情報（config）をJSON形式で格納する文字列
     */
    private String configJson;
    /**
     * 選択中の顔モデルを表す変数
     */
    private int faceNumber = 0;
    /**
     * 選択中の言語を表す変数
     */
    private int languageNumber = 0;
    /**
     * 選択可能な言語名のリストを格納する変数
     */
    private List<String> languageList = new ArrayList<>();
    /**
     * 追加言語情報をJSON形式で格納する文字列
     */
    private String addLanguageJson;
    /**
     * 追加言語で使われる音声ファイルをファイル名と対応させて格納するリスト
     */
    private Map<String, SendFileInfo> addLanguageAudioFiles;
    /**
     * 言語追加の際の音声ファイルの数
     */
    private int countAudioFiles;
    /**
     * 言語追加の際の音声ファイルの総容量
     */
    private long allAudioSize;
    /**
     * 言語削除の対象となるリスト中のインデックス
     */
    private int removeLanguageIndex = -1;
    /**
     * ビープモード（警告を音声ではなくビープ音により行うモード）のON/OFFを表す変数
     */
    private boolean beepMode = true;
    /**
     * 難易度レベルを表す変数
     */
    private int level = 0;
    /**
     * OpenAI APIキーを格納する文字列
     */
    private String openAIAPIKey = "";
    /**
     * Azure環境のリージョン情報を表す文字列
     */
    private String azureRegion = "japanwest";
    /**
     * Azure環境のサブスクリプションキーを格納する文字列
     */
    private String azureSubscriptionKey = "";

    //各設定項目においてサポートアプリ上で変更のリクエストが行われたことを表すフラグ変数群
    /**
     * 評価モードの変更リクエストを表すフラグ
     */
    private boolean triggerChangeEvaluation = false;
    /**
     * リセットモードの変更リクエストを表すフラグ
     */
    private boolean triggerChangeReset = false;
    /**
     * シナリオ/トレーニングモード変更リクエストを表すフラグ
     */
    private boolean triggerChangeMode = false;
    /**
     * 選択シナリオ変更リクエストを表すフラグ
     */
    private boolean triggerChangeScenario = false;
    /**
     * 現在のシナリオ中の進行ステートを進めるリクエストを表すフラグ
     */
    private boolean triggerForwardState = false;
    /**
     * 現在のシナリオ中の進行ステートを戻すリクエストを表すフラグ
     */
    private boolean triggerBackwardState = false;
    /**
     * 設定のエクスポートのリクエストを表すフラグ
     */
    private boolean triggerExportConfig = false;
    /**
     * 設定のインポートのリクエストを表すフラグ
     */
    private boolean triggerImportConfig = false;
    /**
     * 顔モデルの変更リクエストを表すフラグ
     */
    private boolean triggerChangeFace = false;
    /**
     * 選択言語の変更リクエストを表すフラグ
     */
    private boolean triggerChangeLanguage = false;
    /**
     * 言語の追加リクエストを表すフラグ
     */
    private boolean triggerAddLanguage = false;
    /**
     * ビープモード変更リクエストを表すフラグ
     */
    private boolean triggerChangeBeep = false;
    /**
     * 難易度レベル変更リクエストを表すフラグ
     */
    private boolean triggerChangeLevel = false;
    /**
     * OpenAIパラメータ変更リクエストを表すフラグ
     */
    private boolean triggerChangeOpenAIParam = false;
    /**
     * Azure設定パラメータ変更リクエストを表すフラグ
     */
    private boolean triggerChangeAzureParam = false;
    /**
     * 評価リスト取得リクエストを表すフラグ
     */
    private boolean triggerGetEvaluationList = false;
    /**
     * 評価データ取得リクエストを表すフラグ（リクエストがある場合、取得する評価データのファイル名がセットされる）
     */
    private String triggerGetEvaluationData = "";

    //設定項目が変更中（こちら側から変更リクエスト送信済みでゴーグル側での状態変更・リクエストへの返信を待っている状態）
    //であることを表すフラグ群
    //このフラグが立っている状態の項目は新たな変更リクエストを受け付けない
    /**
     * 評価モード変更反映中を表すフラグ
     */
    private boolean changingEvaluation = false;
    /**
     * リセットモード変更反映中を表すフラグ
     */
    private boolean changingReset = false;
    /**
     * シナリオ/トレーニングモードの変更反映中を表すフラグ
     */
    private boolean changingMode = false;
    /**
     * 選択シナリオの変更反映中を表すフラグ
     */
    private boolean changingScenario = false;
    /**
     * 設定のエクスポート待ちを表すフラグ
     */
    private boolean exportingConfig = false;
    /**
     * 顔モデルの変更反映中を表すフラグ
     */
    private boolean changingFace = false;
    /**
     * 選択言語の変更反映中を表すフラグ
     */
    private boolean changingLanguage = false;
    /**
     * ビープモードの変更反映中を表すフラグ
     */
    private boolean changingBeep = false;
    /**
     * 難易度レベルの変更反映中を表すフラグ
     */
    private boolean changingLevel = false;
    /**
     * OpenAIパラメータ変更反映中を表すフラグ
     */
    private boolean changingOpenAIParam = false;
    /**
     * Azureパラメータ変更反映中を表すフラグ
     */
    private boolean changingAzureParam = false;
    /**
     * 評価リストの返送待ちを表すフラグ
     */
    private boolean gettingEvaluationList = false;
    /**
     * 評価データの返送まちを表すフラグ
     */
    private boolean gettingEvaluationData = false;

    /**
     * こちら側からゴーグルへ送る変更リクエスト状況を記述したJSONの取得
     * @return ゴーグル側へ送るJSON文字列
     */
    public synchronized String getRequestingJson() {
        String json = "";

        try {
            JSONObject json_out = new JSONObject();
            json_out.put("evaluation", triggerChangeEvaluation);
            json_out.put("reset", triggerChangeReset);
            json_out.put("mode", triggerChangeMode);
            json_out.put("senario", triggerChangeScenario);
            json_out.put("senarionum", scenarioNumber);
            json_out.put("forward", triggerForwardState);
            json_out.put("backword", triggerBackwardState);
            json_out.put("requestEvaluationList", triggerGetEvaluationList);
            json_out.put("requestEvaluation", triggerGetEvaluationData);
            if(triggerImportConfig && configJson != null) {
                JSONObject config = new JSONObject(configJson);
                json_out.put("importConfig", config);
            }
            json_out.put("requestExportConfig", triggerExportConfig);
            json_out.put("face", (triggerChangeFace ? faceNumber : -1));
            json_out.put("language", false);
            json_out.put("languageIndexChange", triggerChangeLanguage);
            json_out.put("languageIndex", languageNumber);
            if(triggerAddLanguage && addLanguageJson != null) {
                JSONObject addlang = new JSONObject(addLanguageJson);
                json_out.put("addLanguageSetting", addlang);
            }
            if(removeLanguageIndex >= 2)
                json_out.put("removeLanguageIndex", removeLanguageIndex);
            json_out.put("beep", triggerChangeBeep);
            json_out.put("level", -1);
            json_out.put("levelChange", triggerChangeLevel);
            json_out.put("changedLevel", level);
            json_out.put("openAIAPIKey", (triggerChangeOpenAIParam ? openAIAPIKey : ""));
            json_out.put("azureRegion", (triggerChangeAzureParam ? azureRegion : ""));
            json_out.put("azureSubscriptionKey", (triggerChangeAzureParam ? azureSubscriptionKey : ""));
            json = json_out.toString();
        } catch (JSONException err) {
            System.out.println("Exception : " + err.toString());
        }

        triggerChangeEvaluation = false;
        if(triggerChangeReset)
            changingReset = true;
        triggerChangeReset = false;
        if(triggerChangeMode)
            changingMode = true;
        triggerChangeMode = false;
        if (triggerChangeScenario)
            changingScenario = true;
        triggerChangeScenario = false;
        triggerForwardState = false;
        triggerBackwardState = false;
        if (triggerChangeFace)
            changingFace = true;
        triggerChangeFace = false;
        if (triggerChangeLanguage)
            changingLanguage = true;
        triggerChangeLanguage = false;
        triggerAddLanguage = false;
        removeLanguageIndex = -1;
        addLanguageJson = null;
        if (triggerChangeBeep)
            changingBeep = true;
        triggerChangeBeep = false;
        if (triggerChangeLevel)
            changingLevel = true;
        triggerChangeLevel = false;
        if(triggerChangeOpenAIParam)
            changingOpenAIParam = true;
        triggerChangeOpenAIParam = false;
        if(triggerChangeAzureParam)
            changingAzureParam = true;
        triggerChangeAzureParam = false;

        return json;
    }

    /**
     * 評価モードのトグル(ON/OFF切り替え)を行う
     * @param requestFlag ゴーグル側への変更リクエストを行う場合（変更がこちらのアプリ側で行われた場合）trueとする
     * @param runListener リスナーを動作させる場合（UIに変更状況を反映させる場合）
     * @return 変更後の評価モード
     */
    public synchronized boolean toggleEvaluationMode(boolean requestFlag, boolean runListener) {
        return setEvaluationMode(!evaluationMode, requestFlag, runListener);
    }

    /**
     * 評価モードの変更を行う
     * @param mode 変更させる評価モードの状態
     * @param requestFlag ゴーグル側への変更リクエストを行う場合（変更がこちらのアプリ側で行われた場合）trueとする
     * @param runListener リスナーを動作させる場合（UIに変更状況を反映させる場合）
     * @return 変更後の評価モード
     */
    public synchronized boolean setEvaluationMode(boolean mode, boolean requestFlag, boolean runListener) {
        if(triggerChangeEvaluation)
            return evaluationMode;
        else if(evaluationMode != mode) {
            evaluationMode = mode;
            triggerChangeEvaluation = requestFlag;

            if(!evaluationMode && !requestFlag)
                triggerGetEvaluationList = true;

            if(evaluationModeListener != null && runListener)
                evaluationModeListener.onSetBooleanValue(evaluationMode);
        }

        return evaluationMode;
    }

    /**
     * 現在の評価モードを取得する
     * @return 現在の評価モード
     */
    public synchronized boolean getEvaluationMode() { return evaluationMode; }

    /**
     * リセットモードのトグル（ON/OFF切り替え）を行う
     * @param requestFlag ゴーグル側への変更リクエストを行う場合（変更がこちらのアプリ側で行われた場合）trueとする
     * @param runListener リスナーを動作させる場合（UIに変更状況を反映させる場合）
     * @return 変更後のリセットモードの状態
     */
    public synchronized boolean toggleResetMode(boolean requestFlag, boolean runListener) {
        return setResetMode(!resetMode, requestFlag, runListener);
    }

    /**
     * リセットモードの変更を行う
     * @param mode 変更させるリセットモードの状態
     * @param requestFlag ゴーグル側への変更リクエストを行う場合（変更がこちらのアプリ側で行われた場合）trueとする
     * @param runListener リスナーを動作させる場合（UIに変更状況を反映させる場合）
     * @return 変更後のリセットモードの状態
     */
    public synchronized boolean setResetMode(boolean mode, boolean requestFlag, boolean runListener) {
        if(triggerChangeReset)
            return resetMode;
        else if(changingReset) {
            if(resetMode == mode)
                changingReset = false;
        } else if(resetMode != mode) {
            resetMode = mode;
            triggerChangeReset = requestFlag;

            if(resetModeListener != null && runListener)
                resetModeListener.onSetBooleanValue(resetMode);
        }
        return resetMode;
    }

    /**
     * 現在のリセットモードの状態を取得する
     * @return 現在のリセットモード
     */
    public synchronized boolean getResetMode() { return resetMode; }

    /**
     * シナリオ/トレーニングモードの設定を行う
     * @param mode シナリオモードならtrue, トレーニングモードならfalseとする
     * @param requestFlag ゴーグル側への変更リクエストを行う場合（変更がこちらのアプリ側で行われた場合）trueとする
     * @param runListener リスナーを動作させる場合（UIに変更状況を反映させる場合）
     * @return 変更後のモード
     */
    public synchronized boolean setScenarioMode(boolean mode, boolean requestFlag, boolean runListener) {
        if(triggerChangeMode)
            return scenarioMode;
        else if(changingMode) {
            if(scenarioMode == mode)
                changingMode = false;
        } else if(scenarioMode != mode) {
            scenarioMode = mode;
            triggerChangeMode = requestFlag;

            if(scenarioModeListener != null && runListener)
                scenarioModeListener.onSetBooleanValue(scenarioMode);
        }
        return scenarioMode;
    }

    /**
     * 現在のモードを取得する
     * @return 現在のモード（シナリオモードならtrue, トレーニングモードならfalse）
     */
    public synchronized boolean getScenarioMode() { return scenarioMode; }

    /**
     * 選択シナリオの変更を行う
     * @param scenarioNumber 変更後のシナリオインデックス
     * @param requestFlag ゴーグル側への変更リクエストを行う場合（変更がこちらのアプリ側で行われた場合）trueとする
     * @param runListener リスナーを動作させる場合（UIに変更状況を反映させる場合）
     */
    public synchronized void setScenarioNumber(int scenarioNumber, boolean requestFlag, boolean runListener) {
        if(triggerChangeScenario)
            return;
        else if(changingScenario) {
            if(this.scenarioNumber == scenarioNumber)
                changingScenario = false;
        } else if(scenarioMode && this.scenarioNumber != scenarioNumber) {
            this.scenarioNumber = scenarioNumber;
            triggerChangeScenario = requestFlag;

            if(scenarioNumberListener != null && runListener)
                scenarioNumberListener.onSetIntegerValue(this.scenarioNumber);
        }
    }

    /**
     * 選択中のシナリオインデックスを取得する
     * @return 現在選択中のシナリオインデックス
     */
    public synchronized int getScenarioNumber() { return scenarioNumber; }

    /**
     * 現在のシナリオ中の進行ステートを進めるリクエストを送る
     */
    public synchronized void setScenarioStateForward() {
        triggerForwardState = true;
        triggerBackwardState = false;
    }

    /**
     * 現在のシナリオ中の進行ステートを戻すリクエストを送る
     */
    public synchronized void setScenarioStateBackward() {
        triggerForwardState = false;
        triggerBackwardState = true;
    }

    /**
     * 現在のシナリオ中の進行ステートを設定する
     * @param state 現在のシナリオ中の進行ステートのインデックス
     */
    public synchronized void setScenarioState(int state) { currentState = state; }

    /**
     * 現在のシナリオ中の進行ステートを取得する
     * @return 現在のシナリオ中の進行ステートのインデックス
     */
    public synchronized int getScenarioState() { return currentState; }

    /**
     * 評価リストの取得リクエストを送る
     */
    public synchronized void setRequestEvaluationList() {
        triggerGetEvaluationList = true;
    }

    /**
     * 評価リストを設定する（ゴーグル側からの受信時に呼び出す前提）
     * @param evaluations 評価リストの配列（評価データのファイル名の配列）
     */
    public synchronized void setEvaluationList(String[] evaluations) {
        if(evaluationListListener != null)
            evaluationListListener.onChangedStringList(evaluations);
        triggerGetEvaluationList = false;
    }

    /**
     * 評価データ取得リクエストを送る
     * @param evaluation_file 取得したい評価データのファイル名
     */
    public synchronized void setRequestEvaluation(String evaluation_file) {
        triggerGetEvaluationData = evaluation_file;
    }

    /**
     * 受信した評価データをリスナーに送る
     * @param evaluation_json
     */
    public synchronized void setEvaluationData(String evaluation_json) {
        if(evaluationListener != null)
            evaluationListener.onSetStringValue(evaluation_json);
        triggerGetEvaluationData = "";
    }

    /**
     * 設定のインポートを行う
     * @param importedConfigJson インポートする設定が記述されたJSON文字列
     */
    public synchronized void importConfig(String importedConfigJson) {
        this.configJson = importedConfigJson;
        triggerImportConfig = true;
    }

    /**
     * 設定のインポートの完了を通知する（インポート反映待ちを解除する）
     */
    public synchronized void importedConfig() {
        triggerImportConfig = false;
        configJson = "";
        if(importedConfigListener != null)
            importedConfigListener.onSetBooleanValue(true);
    }

    /**
     * 設定のエクスポートのリクエストを送る
     */
    public synchronized void requestExportConfig() {
            triggerExportConfig = true;
    }

    /**
     * 受信した設定データからエクスポート処理開始を行う
     * @param config 受信した設定データ
     */
    public synchronized void exportConfig(String config) {
        configJson = config;

        if(exportConfigListener != null)
            exportConfigListener.onSetStringValue(configJson);

        triggerExportConfig = false;
    }

    /**
     * 顔モデルの変更を行う
     * @param faceNumber 変更後の顔モデルのインデックス
     * @param requestFlag ゴーグル側への変更リクエストを行う場合（変更がこちらのアプリ側で行われた場合）trueとする
     * @param runListener リスナーを動作させる場合（UIに変更状況を反映させる場合）
     */
    public synchronized void setFaceNumber(int faceNumber, boolean requestFlag, boolean runListener) {
        if(triggerChangeFace)
            return;
        else if(changingFace) {
            if(this.faceNumber == faceNumber)
                changingFace = false;
        } else if(this.faceNumber != faceNumber) {
            this.faceNumber = faceNumber;
            triggerChangeFace = requestFlag;

            if(faceListener != null && runListener)
                faceListener.onSetIntegerValue(this.faceNumber);
        }
    }

    /**
     * 現在の顔モデルを取得する
     * @return 現在選択中の顔モデルのインデックス
     */
    public synchronized int getFaceNumber() { return faceNumber; }

    /**
     * 顔モデルの変更を行う
     * @param languageNumber 変更後の顔モデルのインデックス
     * @param requestFlag ゴーグル側への変更リクエストを行う場合（変更がこちらのアプリ側で行われた場合）trueとする
     * @param runListener リスナーを動作させる場合（UIに変更状況を反映させる場合）
     */
    public synchronized void setLanguageNumber(int languageNumber, boolean requestFlag, boolean runListener) {
        if(triggerChangeLanguage)
            return;
        else if(changingLanguage) {
            if(this.languageNumber == languageNumber)
                changingLanguage = false;
        } else if(this.languageNumber != languageNumber) {
            this.languageNumber = languageNumber;
            triggerChangeLanguage = requestFlag;

            if(languageListener != null && runListener)
                languageListener.onSetIntegerValue(this.languageNumber);
        }
    }

    /**
     * 現在の言語を取得する
     * @return 現在選択中の言語のインデックス
     */
    public synchronized int getLanguageNumber() { return languageNumber; }

    /**
     * 言語のリストを設定する
     * @param languages 選択可能言語の名前を表す文字列を格納した配列
     */
    public void setLanguageList(String[] languages) {
        languageList = Arrays.asList(languages);
        if(languageListListener != null)
            languageListListener.onChangedStringList(languages);
    }

    /**
     * 言語リストを取得する
     * @return 選択可能な言語の名前を表す文字列を格納した配列
     */
    public synchronized List<String> getLanguageList() {
        return languageList;
    }

    /**
     * 言語の追加を行う
     * @param addLanguageJson 追加言語の表示テキストデータ等を記述したJSON文字列
     * @param audioFiles 音声ファイル名をキーとする音声ファイル情報をまとめたMap
     */
    public synchronized void addLanguage(String addLanguageJson, Map<String, SendFileInfo> audioFiles) {
        this.addLanguageJson = addLanguageJson;
        addLanguageAudioFiles = audioFiles;

        countAudioFiles = addLanguageAudioFiles.size();
        allAudioSize = 0;
        for(String filename : addLanguageAudioFiles.keySet())
            allAudioSize += addLanguageAudioFiles.get(filename).sizeInBytes;

        triggerAddLanguage = true;
    }

    /**
     * 言語の追加処理の開始を通知する
     */
    public synchronized void startAddLanguage() {
        if(startAddLanguageListener != null)
            startAddLanguageListener.onSetBooleanValue(true);
    }

    /**
     * 音声ファイル（1ファイル）のアップロード完了を通知する
     * @param filename アップロードが完了した音声ファイルのファイル名
     */
    public synchronized void completedUploadAudioFile(String filename) {
        addLanguageAudioFiles.remove(filename);

        if(uploadLanguageAudioFilesListener != null) {
            long restsize = 0;
            for(String fname : addLanguageAudioFiles.keySet())
                restsize += addLanguageAudioFiles.get(fname).sizeInBytes;
            uploadLanguageAudioFilesListener.onCompletedUploadFile(
                    filename,
                    countAudioFiles - addLanguageAudioFiles.size(),
                    allAudioSize - restsize,
                    countAudioFiles, allAudioSize);
        }
    }

    /**
     * 次にアップロードする音声ファイルを取得する
     * @return 次にアップロードする音声ファイルのファイル名とSendFileInfoのペア（次が無ければnull）
     */
    public synchronized Pair<String, SendFileInfo> getAddLanguageAudioFile() {
        Pair<String, SendFileInfo> pair = null;
        for(String filename : addLanguageAudioFiles.keySet()) {
            pair = new Pair<>(filename, addLanguageAudioFiles.get(filename));
            break;
        }

        return pair;
    }

    /**
     * 言語の追加処理が完了したことを通知する
     */
    public synchronized void addedLanguage() {
        triggerAddLanguage = false;
        countAudioFiles = 0;
        allAudioSize = 0;

        if(completedAddLanguageListener != null)
            completedAddLanguageListener.onSetBooleanValue(true);
    }

    /**
     * 言語の削除を行う
     * @param index 削除する言語のインデックス（0,1は不可）
     */
    public synchronized void removeLanguage(int index) {
        if(index < 2)
            return;
        removeLanguageIndex = index;
    }

    /**
     * 言語の削除の完了を通知する
     */
    public synchronized void removedLanguage() {
        if(removedLanguageListener != null)
            removedLanguageListener.onSetBooleanValue(true);
    }

    /**
     * ビープモードのトグル（ON/OFF切り替え）を行う
     * @param requestFlag ゴーグル側への変更リクエストを行う場合（変更がこちらのアプリ側で行われた場合）trueとする
     * @param runListener リスナーを動作させる場合（UIに変更状況を反映させる場合）
     * @return 変更後のビープモード
     */
    public synchronized boolean toggleBeepMode(boolean requestFlag, boolean runListener) {
        return setBeepMode(!beepMode, requestFlag, runListener);
    }

    /**
     * ビープモードの変更を行う
     * @param beepMode 変更後のビープモード
     * @param requestFlag ゴーグル側への変更リクエストを行う場合（変更がこちらのアプリ側で行われた場合）trueとする
     * @param runListener リスナーを動作させる場合（UIに変更状況を反映させる場合）
     * @return 変更後のビープモード
     */
    public synchronized boolean setBeepMode(boolean beepMode, boolean requestFlag, boolean runListener) {
        if(triggerChangeBeep)
            return this.beepMode;
        else if(changingBeep) {
            if(this.beepMode == beepMode)
                changingBeep = false;
        } else if(this.beepMode != beepMode) {
            this.beepMode = beepMode;
            triggerChangeBeep = requestFlag;

            if(beepModeListener != null && runListener)
                beepModeListener.onSetBooleanValue(this.beepMode);
        }

        return this.beepMode;
    }

    /**
     * 現在のビープモードを取得
     * @return 現在のビープモードの値
     */
    public synchronized boolean getBeepMode() { return beepMode; }

    /**
     * 難易度レベルの変更を行う
     * @param level 変更後の難易度レベル（0:Easy, 1:Medium, 2:Hard）
     * @param requestFlag ゴーグル側への変更リクエストを行う場合（変更がこちらのアプリ側で行われた場合）trueとする
     * @param runListener リスナーを動作させる場合（UIに変更状況を反映させる場合）
     */
    public synchronized void setLevel(int level, boolean requestFlag, boolean runListener) {
        if(triggerChangeLevel)
            return;
        else if(changingLevel) {
            if(this.level == level)
                changingLevel = false;
        } else if(this.level != level) {
            this.level = level;
            triggerChangeLevel = requestFlag;

            if(levelListener != null && runListener)
                levelListener.onSetIntegerValue(this.level);
        }
    }

    /**
     * 現在の難易度レベルを取得する
     * @return 現在の難易度レベルを表す数値（0:Easy, 1:Medium, 2:Hard）
     */
    public synchronized int getLevel() { return level; }

    /**
     * OpenAIのAPIキーを設定する
     * @param openAIAPIKey OpenAI用のAPIキーを表す文字列
     * @param requestFlag ゴーグル側への変更リクエストを行う場合（変更がこちらのアプリ側で行われた場合）trueとする
     * @param runListener リスナーを動作させる場合（UIに変更状況を反映させる場合）
     */
    public synchronized void setOpenAIKey(String openAIAPIKey, boolean requestFlag, boolean runListener) {
        if(triggerChangeOpenAIParam)
            return;
        else if(changingOpenAIParam) {
            if(this.openAIAPIKey.equals(openAIAPIKey))
                changingOpenAIParam = false;
        } else if(!this.openAIAPIKey.equals(openAIAPIKey)) {
            this.openAIAPIKey = openAIAPIKey;
            triggerChangeOpenAIParam = requestFlag;

            if(openAIAPIKeyListener != null && runListener)
                openAIAPIKeyListener.onSetStringValue(this.openAIAPIKey);
        }
    }

    /**
     * 現在設定されているOpenAI用のAPIキーを取得する
     * @return OpenAI用APIキーを表す文字列
     */
    public synchronized String getOpenAIKey() { return openAIAPIKey; }

    /**
     * Azureのパラメータ設定を行う
     * @param azureRegion Azureのリージョンを表す文字列
     * @param azureSubscriptionKey Azureのサブスクリプションキーを表す文字列
     * @param requestFlag ゴーグル側への変更リクエストを行う場合（変更がこちらのアプリ側で行われた場合）trueとする
     * @param runListener リスナーを動作させる場合（UIに変更状況を反映させる場合）
     */
    public synchronized void setAzureSettings(String azureRegion, String azureSubscriptionKey, boolean requestFlag, boolean runListener) {
        if(triggerChangeAzureParam)
            return;
        else if(changingAzureParam) {
            if(this.azureRegion.equals(azureRegion) && this.azureSubscriptionKey.equals(azureSubscriptionKey))
                changingAzureParam = false;
        } else if(!this.azureRegion.equals(azureRegion) || !this.azureSubscriptionKey.equals(azureSubscriptionKey)) {
            this.azureRegion = azureRegion;
            this.azureSubscriptionKey = azureSubscriptionKey;
            triggerChangeAzureParam = requestFlag;

            if(azureSettingsListener != null && runListener)
                azureSettingsListener.onChangedStringList(new String[] {this.azureRegion, this.azureSubscriptionKey});
        }
    }

    /**
     * 現在設定中のAzureのリージョン設定を取得する
     * @return Azureのリージョン設定を表す文字列
     */
    public synchronized String getAzureRegion() { return azureRegion; }

    /**
     * 現在設定中のAzureのAzureのサブスクリプションキーを取得する
     * @return Azureのサブスクリプションキーを表す文字列
     */
    public synchronized String getAzureSubscriptionKey() { return azureSubscriptionKey; }

    /**
     * 評価モード変更リスナー
     */
    OnSetBooleanValueListener evaluationModeListener;

    /**
     * 評価モード変更リスナーを設定する
     * @param listener 設定する評価モード変更リスナー
     */
    public void setEvaluationModeListener(OnSetBooleanValueListener listener) {
        evaluationModeListener = listener;
    }

    /**
     * リセットモード変更リスナー
     */
    OnSetBooleanValueListener resetModeListener;

    /**
     * リセットモード変更リスナーを設定する
     * @param listener 設定するリセットモード変更リスナー
     */
    public void setResetModeListener(OnSetBooleanValueListener listener) {
        resetModeListener = listener;
    }

    /**
     * シナリオ/トレーニングモード変更リスナー
     */
    OnSetBooleanValueListener scenarioModeListener;

    /**
     * シナリオ/トレーニングモード変更リスナーを設定する
     * @param listener 設定するシナリオ/トレーニングモード変更リスナー
     */
    public void setScenarioModeListener(OnSetBooleanValueListener listener) {
        scenarioModeListener = listener;
    }

    /**
     * シナリオ変更リスナー
     */
    OnSetIntegerValueListener scenarioNumberListener;

    /**
     * シナリオ変更リスナーを設定する
     * @param listener 設定するシナリオ変更リスナー
     */
    public void setScenarioNumberListener(OnSetIntegerValueListener listener) {
        scenarioNumberListener = listener;
    }

    /**
     * 評価リスト受信リスナー
     */
    OnChangedStringListListener evaluationListListener;

    /**
     * 評価リスト受信リスナーを設定する
     * @param listener 設定する評価リスト受信リスナー
     */
    public void setEvaluationListListener(OnChangedStringListListener listener) {
        evaluationListListener = listener;
    }

    /**
     * 評価データ受信リスナー
     */
    OnSetStringValueListener evaluationListener;

    /**
     * 評価データ受信リスナーを設定する
     * @param listener 設定する評価データ受信リスナー
     */
    public void setEvaluationDataListener(OnSetStringValueListener listener) {
        evaluationListener = listener;
    }

    /**
     * 設定インポート完了リスナー
     */
    OnSetBooleanValueListener importedConfigListener;

    /**
     * 設定インポート完了リスナーを設定する
     * @param listener 設定する設定インポート完了リスナー
     */
    public void setImportedConfigListener(OnSetBooleanValueListener listener) {
        importedConfigListener = listener;
    }

    /**
     * エクスポート設定受信リスナー
     */
    OnSetStringValueListener exportConfigListener;

    /**
     * エクスポート設定受信リスナーを設定する
     * @param listener 設定するエクスポート設定受信リスナー
     */
    public void setExportConfigListener(OnSetStringValueListener listener) {
        exportConfigListener = listener;
    }

    /**
     * 顔モデル変更リスナー
     */
    OnSetIntegerValueListener faceListener;

    /**
     * 顔モデル変更リスナーを設定する
     * @param listener 設定する顔モデル変更リスナー
     */
    public void setFaceListener(OnSetIntegerValueListener listener) {
        faceListener = listener;
    }

    /**
     * 言語変更リスナー
     */
    OnSetIntegerValueListener languageListener;

    /**
     * 言語変更リスナーを設定する
     * @param listener 設定する言語変更リスナー
     */
    public void setLanguageListener(OnSetIntegerValueListener listener) {
        languageListener = listener;
    }

    /**
     * 言語リスト変更リスナー
     */
    OnChangedStringListListener languageListListener;

    /**
     * 言語リスト変更リスナーを設定する
     * @param listener 設定する言語リストリスナー
     */
    public void setLanguageListListener(OnChangedStringListListener listener) {
        languageListListener = listener;
    }

    /**
     * 言語追加開始リスナー
     */
    OnSetBooleanValueListener startAddLanguageListener;

    /**
     * 言語追加開始リスナーを設定する
     * @param listener 設定する言語追加開始リスナー
     */
    public void setStartAddLanguageListener(OnSetBooleanValueListener listener) {
        startAddLanguageListener = listener;
    }

    /**
     * 言語音声ファイルアップロード進捗リスナー
     */
    OnFileUploadProgressListener uploadLanguageAudioFilesListener;

    /**
     * 言語音声ファイルアップロード進捗リスナーを設定する
     * @param listener 設定する言語音声ファイルアップロード進捗リスナー
     */
    public void setUploadLanguageAudioFilesListener(OnFileUploadProgressListener listener) {
        uploadLanguageAudioFilesListener = listener;
    }

    /**
     * 言語追加完了リスナー
     */
    OnSetBooleanValueListener completedAddLanguageListener;

    /**
     * 言語追加完了リスナーを設定する
     * @param listener 設定する言語追加完了通知リスナー
     */
    public void setCompletedAddLanguageListener(OnSetBooleanValueListener listener) {
        completedAddLanguageListener = listener;
    }

    /**
     * 言語削除リスナー
     */
    OnSetBooleanValueListener removedLanguageListener;

    /**
     * 言語削除リスナーを設定する
     * @param listener 設定する言語削除リスナー
     */
    public void setRemovedLanguageListener(OnSetBooleanValueListener listener) {
        removedLanguageListener = listener;
    }

    /**
     * ビープモード変更リスナー
     */
    OnSetBooleanValueListener beepModeListener;

    /**
     * ビープモード変更リスナーを設定する
     * @param listener 設定するビープモード変更リスナー
     */
    public void setBeepModeListener(OnSetBooleanValueListener listener) {
        beepModeListener = listener;
    }

    /**
     * 難易度レベル変更リスナー
     */
    OnSetIntegerValueListener levelListener;

    /**
     * 難易度レベル変更リスナーを設定する
     * @param listener 設定する難易度レベル変更リスナー
     */
    public void setLevelListener(OnSetIntegerValueListener listener) {
        levelListener = listener;
    }

    /**
     * OpenAI用パラメータ変更リスナー
     */
    OnSetStringValueListener openAIAPIKeyListener;

    /**
     * OpenAI用パラメータ変更リスナーを設定する
     * @param listener 設定するOpenAI用パラメータ変更リスナー
     */
    public void setOpenAIAPIKeyListener(OnSetStringValueListener listener) {
        openAIAPIKeyListener = listener;
    }

    /**
     * Azure用パラメータ変更リスナー
     */
    OnChangedStringListListener azureSettingsListener;

    /**
     * Azure用パラメータ変更リスナー
     * @param listener 設定するAzure用パラメータ変更リスナー
     */
    public void setAzureSettingsListener(OnChangedStringListListener listener) {
        azureSettingsListener = listener;
    }
}
