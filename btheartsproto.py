import sys
import bluetooth
import json
import time
import threading
import ctypes

# Python Library for HEARTS protocol
# ver. 0.1
class BLEHeartsEvaListener(threading.Thread):
    def __init__(self, sock):
        super(BLEHeartsEvaListener, self).__init__()
        self.stop_thread = False
        self.sock = sock
        self.buffer = []
        text = '{"evaluation":true, "reset":false,"mode":false,"senario":false,"senarionum":0,"forward":false,"backword":false,"requestEvaluationList":false,"requestEvaluation":"","requestExportConfig":false,"face":-1,"language":false,"languageIndexChange":false,"languageIndex":0,"beep":false,"level":-1,"levelChange":false,"changedLevel":0}'
        self.json_default = json.loads(text)

    def stop(self):
        self.json_default['evaluation'] = 'false'
        text = json.dumps(self.json_default)
        length = len(text)
        len_bytes = length.to_bytes(4,'big')
        self.sock.send(len_bytes)
        self.sock.send(text)
        self.stop_thread = True
        time.sleep(0.1)
        self.raise_exception()

    # Fuction to terminate thread
    def raise_exception(self):
        thread_id = -1
        for id, thread in threading._active.items():
            if thread is self:
                thread_id = id
                break
        resu = ctypes.pythonapi.PyThreadState_SetAsyncExc(ctypes.c_long(thread_id), ctypes.py_object(SystemExit))
        if resu > 1:
            ctypes.pythonapi.PyThreadState_SetAsyncExc(ctypes.c_long(thread_id), 0)
            print('Failure in raising exception')

    def decodemessage(self, mes):
        # "{\"CurrentTime\":\"20220706_174452\",\"TotalTime\":0.5088662505149841,\"MultiModalTime\":0.0,\"VoiceTime\":0.5088662505149841,
        # \"EyeContactTime\":0.0,\"GazeTime\":0.0,\"TouchTime\":0.0,\"VoiceScore\":100.0,\"EyeContactScore\":0.0,\"TouchScore\":0.0,
        # \"GazeScore\":0.0,\"MultiModalScore\":0.0,\"TouchState\":-1,\"VoiceState\":1,\"EyeContactState\":0,\"GazeState\":0,
        # \"Distance\":96.70360565185547,\"evaluation\":false,\"reset\":false,\"language\":false,\"beep\":false,\"mode\":false,
        # \"forward\":false,\"backword\":false,\"senario\":false,\"senarionum\":0,\"level\":0}";
        data = json.loads(mes)
        self.buffer.append(data)

    def save_json(self, fname):
        # save buffer data to json
        try:
            with open(fname, mode="wt", encoding="utf-8") as f:
	            json.dump(self.buffer, f, ensure_ascii=False, indent=2)
        except:
            print("couldnot save:", fname)        
        
    def run(self):
        while(True):
            # send message
            self.json_default['evaluation'] = 'true'
            text = json.dumps(self.json_default)         
            length = len(text)
            len_bytes = length.to_bytes(4,'big')
            self.sock.send(len_bytes)
            self.sock.send(text)

            #print("now receiving")
            # receive message
            data = self.sock.recv(4)
            size = int.from_bytes(data, 'big')
            # print('size of received data: ', size)

            full_msg = b''
            while(True):
                d = self.sock.recv(100)
                if len(d) <= 0:
                    break
                full_msg += d

                if len(full_msg) == size:
                    break

            msg = full_msg.decode('utf-8')
            self.decodemessage(msg)

            if self.stop_thread == True:
                self.stop_thread = False
                break

            time.sleep(0.1)

class BLEHearts():
    def __init__(self):
        # UUID of HEARTS application
        self.uuid = "41eb5f39-6c3a-4067-8bb9-bad64e6e0908"
        # device list
        self.devices = {}
        # socket connected status
        self.connected = False
        # evaluation listener
        self.listener = False

    def scan_devices(self):
        devices = bluetooth.discover_devices(lookup_class=False, lookup_names=True)
        # service = bluetooth.find_service()
        # devices = service.discover(2)
        for x in devices:
            if x[1] not in self.devices.keys():
                self.devices[x[1]] = [x[0],x[1]]

    def load_devices(self):
        try:
            with open("devices.json", mode="rt", encoding="utf-8") as f:
                self.devices = json.load(f)
        except:
            print("couldnt load devices.json")

    def save_devices(self):
        try:
            with open("devices.json", mode="wt", encoding="utf-8") as f:
	            json.dump(self.devices, f, ensure_ascii=False, indent=2)
        except:
            print("couldnot save devices.json")

    def connect(self,addr):
        # service_matches = bluetooth.find_service(uuid = self.uuid)
        service_matches = bluetooth.find_service(address = addr, uuid = self.uuid)

        if len(service_matches) == 0:
            return False
        
        # print(service_matches)
        first_match = service_matches[0]
        self.port = first_match["port"]
        self.host = first_match["host"]

        self.sock = bluetooth.BluetoothSocket( bluetooth.RFCOMM )
        try:
            self.sock.connect((self.host, self.port))
            print("connected to ", self.host, self.port)
            self.connected = True
        except Exception as e:
            print("connection failed")
            self.connected = False
        
        return self.connected

    def start_evaluation(self):
        # print("connection status: ", self.connected)
        if self.connected == False:
            return False

        # Preapre listener and start
        self.listener = BLEHeartsEvaListener(self.sock)
        self.listener.start()

    def stop_evaluation(self):
        if self.connected == False:
            return False

        self.listener.stop()

    def clear_eval_data(self):
        if self.listener == False:
            return False

        self.listener.buffer = []

    def get_evaluation_data(self):
        if self.listener:
            return self.listener.buffer
        else:
            return False
        
def main():
    bleh = BLEHearts()

    #亀川
    # device scan functions
    #
    bleh.load_devices()
    #print("now scanning devices")
    #bleh.scan_devices()
    print(bleh.devices)
    #bleh.save_devices()

    if bleh.connect('A0:85:FC:2E:E9:3F'):
        print("connected")
    else:
        print("cannot connect to the device/uuid")
        sys.exit(0)

    print("start evaluation")
    bleh.start_evaluation()

    try:
        for i in range(600):
            data = bleh.get_evaluation_data()
            # "VoiceScore\":100.0,\"EyeContactScore\":0.0,\"TouchScore\":0.0,\"GazeScore\":0.0,\"MultiModalScore\":0.0,\"TouchState\":-1,\"VoiceState\":1,\"EyeContactState\":0,\"GazeState\":0,\"Distance\":96.70360565185547,
            curdata = bleh.get_evaluation_data()
            if len(curdata) > 0:
                print('EyeContactState:', curdata[-1]['EyeContactState'], end="")
                print('GazeState      :', curdata[-1]['GazeState'], end="")
                print('VoiceState     :', curdata[-1]['VoiceState'], end="")
                print('Distance       :', curdata[-1]['Distance'], end="")       
                print('\r')
            time.sleep(0.1)
    except KeyboardInterrupt:
        print("break")

    bleh.stop_evaluation()

if __name__ == "__main__":
    main()
