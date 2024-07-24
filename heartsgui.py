import PySimpleGUI as sg
from btheartsproto import BLEHearts, BLEHeartsEvaListener
import threading
import time
import os
import datetime

# BLuetooth Hearts Class
bleh = BLEHearts()

# evaluating or not
evaluating = False

# window
window = ""

# data save directory
DIR = ""

# connected device
DEVICE = ""

# version
VERSION = "HEARTS GUI ver.0.13 / HCI Lab, OKAYAMA UNIV."

# 状態をアップデートする関数(Thread)
def eval_update_func(window):
    global evaluating

    while True:
        data = bleh.get_evaluation_data()

        if len(data) > 0:
            if data[-1]['EyeContactState'] == 1:
                window.write_event_value(('EYECONTACT', 'TRUE'), 'TRUE')
            else:
                window.write_event_value(('EYECONTACT', 'FALSE'), 'FALSE')

            if data[-1]['VoiceState'] == 1:
                window.write_event_value(('VOICE', 'TRUE'), 'TRUE')
            else:
                window.write_event_value(('VOICE', 'FALSE'), 'FALSE')       
            
            if data[-1]['VoiceState'] == 1 and data[-1]['EyeContactState'] == 1:
                window.write_event_value(('MULTI', 'TRUE'), 'TRUE')
            else:
                window.write_event_value(('MULTI', 'FALSE'), 'FALSE')

            # 評価中であれば評価値を更新する
            if evaluating:
                estat = get_evaluation_stat()
                if estat is not False:
                    # estat consists of [ti, num_ec/len(data), num_voice/len(data), num_gaze/len(data), sum_dist/len(data)]
                    window.write_event_value(('EVAL_UPDATE', estat[0], estat[1], estat[2], estat[3], estat[4]), 'TRUE')

            window.write_event_value(('DISTANCE', data[-1]['Distance']), 'FALSE')

        time.sleep(0.01)

        #if evaluating == False:
        #    break

# ステータスバーにメッセージを表示
def statusmessage(text):
    global window
    window.write_event_value(('STATUSMESSAGE', text), 'TRUE')

# セッションのデータをファイルに保存する
def save_session_file(session):
    if bleh.listener is not False:
        bleh.listener.save_json(f'{session}.json')
    else:
        statusmessage("evaluation is not performed.")

# 評価の統計値を得る
def get_evaluation_stat():
    if bleh.listener is not False:
        data = bleh.listener.buffer
        if len(data) == 0:
            return False

        ti = int(data[-1]['CurrentTime'].split('_')[1]) - int(data[0]['CurrentTime'].split('_')[1])

        num_ec = 0
        num_voice = 0
        num_multi = 0
        sum_dist = 0
        for d in data:
            if d['EyeContactState'] > 0:
                num_ec += 1
            if d['VoiceState'] > 0:
                num_voice += 1
            if d['EyeContactState'] > 0 and d['VoiceState'] > 0:
                num_multi += 1
            sum_dist += float(d['Distance'])

        return [ti, 100.0*num_ec/len(data), 100.0*num_voice/len(data), 100.0*num_multi/len(data), sum_dist/len(data)]
    else:
        return False

def device_scan_func(window):
    bleh.scan_devices()
    bleh.save_devices()

def connect_func(window):
    global DEVICE
    DEVICE = window['combo_device'].get()
    if len(DEVICE) == 0:
        return
    
    addr = bleh.devices[DEVICE][0]
    statusmessage("connecting to %s(%s)"%(DEVICE,addr))
    bleh.connect(addr)

def main():
    global evaluating
    global window
    global DIR, DEVICE

    # load bluetooth devices from json file
    bleh.load_devices()
    sg.theme('Dark Blue 3')

    # list devices
    devices = list(bleh.devices.keys())
    device_default = devices[0] if len(devices) > 0 else []

    # セッション番号
    session = 1

    # 評価データ
    eval_stat = []

    col1 = [[sg.Text('経過時間',font=("Calibri", 20))],
            [sg.Text('アイコンタクトスコア',font=("Calibri", 20))],
            [sg.Text('会話スコア',font=("Calibri", 20))],
            [sg.Text('マルチモーダルスコア',font=("Calibri", 20))],
            [sg.Text('平均顔間距離',font=("Calibri", 20))]]    
    col2 = [[sg.Text('0',key='duration',size=(10,1), font=("Calibri", 20),justification="right")],
            [sg.Text('0',key='ec_score',size=(10,1), font=("Calibri", 20),justification="right")],
            [sg.Text('0',key='voice_score',size=(10,1), font=("Calibri", 20),justification="right")],
            [sg.Text('0',key='mm_score',size=(10,1), font=("Calibri", 20),justification="right")],
            [sg.Text('0',key='dist_av_score',size=(10,1), font=("Calibri", 20),justification="right")]            
            ]
    
    face_text = ["デフォルト", "高齢(男)", "若年(女)", "若年(男)", "高齢(Real)"]

    layout = [
        [sg.Combo(devices,default_value=device_default,key='combo_device',readonly=True,size=(30,24)), 
         sg.Button('接続',key="CONNECT"), sg.Button('スキャン',key="SCAN")],
        [sg.Button('評価開始',key='start_eval', disabled = True), sg.Button('評価停止', disabled = True,key='stop_eval'), 
         sg.Text('フェイス'), sg.Combo(face_text,default_value="デフォルト",readonly=True,enable_events=True,key="FACE")], 
        [sg.HorizontalSeparator()],
        [sg.Text('セッション番号', font=("Calibri", 20)), sg.Text(f'{session}', key='session', font=("Calibri", 20),justification="right")],
        [sg.HorizontalSeparator()],          
        [sg.Text('アイコンタクト',key='eye_contact',font=("Calibri", 20)),sg.Text('会話',key='voice',font=("Calibri", 20)),
         sg.Text('マルチモーダル',key='multi',font=("Calibri", 20))],
        [sg.Column([[sg.Text('顔間距離', font=("Calibri", 20))]]), sg.Column([[sg.Text('0', key='distance', font=("Calibri", 20),justification="right")]])],
        [sg.HorizontalSeparator()],
        [sg.Column(col1), sg.Column(col2)],
        [sg.HorizontalSeparator()],
        [sg.Table([[],[],[],[],[],[]],headings=["ｾｯｼｮﾝ"," 時 間 ","ｱｲｺﾝﾀｸﾄ"," 会 話 ","ﾏﾙﾁﾓｰﾀﾞﾙ"," 距 離 "],key='table')],
        [sg.StatusBar(VERSION,key="statusbar",auto_size_text=False)]
    ] 

    window = sg.Window('HEARTS_GUI', layout, finalize=True)

    while True:
        event, values = window.read()
        # print(event, values)

        if event is None:
            # kill the thread
            bleh.stop_listener()
            # print('exit')
            break

        # 接続を処理する
        if event == 'CONNECT':
            window.start_thread(lambda: connect_func(window), 'CONNECT_END')
            window['CONNECT'].update(disabled = True)

        if event == 'CONNECT_END':  
            if bleh.connected == True:
                window['CONNECT'].update(disabled = True)
                statusmessage("connected") 
                window['start_eval'].update(disabled = False)
                window['stop_eval'].update(disabled = True)
                window.start_thread(lambda: eval_update_func(window), '-THREAD-')
            else:
                window['CONNECT'].update(disabled = False)                    
                statusmessage("connection failed.")

        # Bluetoothのデバイススキャンを処理する
        if event == 'SCAN':
            statusmessage('now updating devices..')
            window.start_thread(lambda: device_scan_func(window), 'SCAN_END')
            window["SCAN"].update(disabled = True)

        if event == 'SCAN_END':
            devices = list(bleh.devices.keys())
            device_default = devices[0] if len(devices) > 0 else []
            window['combo_device'].update(values=devices)
            window["SCAN"].update(disabled = False)
            statusmessage('scan end')            
        
        # 評価開始と停止
        if event == 'start_eval':
            window['start_eval'].update(disabled = True)
            window['stop_eval'].update(disabled = False)
            statusmessage("start evaluation")
            # Listerのデータをクリアする
            bleh.clear_eval_data()
            evaluating = True
            #bleh.start_evaluation()
            # window.start_thread(lambda: eval_update_func(window), '-THREAD-')

        if event == 'stop_eval':
            # evaluatingを止める
            evaluating = False
            # 現在のデータを保存する
            if DIR == "":
                # ディレクトリを作る
                t = datetime.datetime.now()
                DIR = os.path.join(*['data',DEVICE,f'{t.year}{t.month}{t.day}{t.hour}{t.minute}'])
                os.makedirs(DIR)

            FILE = os.path.join(DIR,f"{session:04}.json")
            save_session_file(FILE)
            statusmessage(f'File saved to: {FILE}')

            # テーブルにupdateする
            eval_stat.append([session,window['duration'].get(),window['ec_score'].get(),
                         window['voice_score'].get(),window['mm_score'].get(),
                         window['dist_av_score'].get()])
            window['table'].update(eval_stat)
            
            with open(os.path.join(DIR,'evaluations.txt'), 'w') as f:
                for L in eval_stat:
                    print(*L, sep="\t", file=f) 

            session += 1
            window['start_eval'].update(disabled = False)
            window['stop_eval'].update(disabled = True)            
            window['session'].update(value=f'{session}')

        # スレッドからの状態更新関数
        if event[0] == 'EYECONTACT':
            if event[1] == 'TRUE':
                window['eye_contact'].update(background_color="green")
            if event[1] == 'FALSE':
                window['eye_contact'].update(background_color="red")

        if event[0] == 'VOICE':
            if event[1] == 'TRUE':
                window['voice'].update(background_color="green")
            if event[1] == 'FALSE':
                window['voice'].update(background_color="red")

        if event[0] == 'MULTI':
            if event[1] == 'TRUE':
                window['multi'].update(background_color="green")
            if event[1] == 'FALSE':
                window['multi'].update(background_color="red")

        if event[0] == 'DISTANCE':
            d = float(event[1])
            window['distance'].update(value = "%3.2fcm"%(d))

        if event[0] == 'STATUSMESSAGE':
            window['statusbar'].update(event[1])
            window.refresh()

        # 顔を変更する
        if event == 'FACE':
            face_id = face_text.index(window['FACE'].get())
            bleh.set_face(face_id)

        if event[0] == 'EVAL_UPDATE':
            window['duration'].update(value = f"{event[1]} 秒")
            window['ec_score'].update(value = f"{event[2]:.1f} %")
            window['voice_score'].update(value = f"{event[3]:.1f} %")
            window['mm_score'].update(value = f"{event[4]:.1f} %")
            window['dist_av_score'].update(value = f"{event[5]:.1f} cm")

    window.close()

if __name__ == "__main__":
    main()