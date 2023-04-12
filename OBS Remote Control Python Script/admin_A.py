import obspython as obs
from random import randint
import requests
import json
import re
import websocket
import asyncio
import time
import threading


IP_ADDR = "127.0.0.1"
IP_PORT = "8080"
SID = "ob1"
secret = "123456"


scene_camera_dictionary={
    'PS2CPC 军团战直播': 'ob1',
    'OBS 图测试': 'ob2',
    '戴尔显示器': 'ob3',
    '便携显示器': 'ob4'
}


wsConnect = None



def buildFullUri():
    return "ws://"+IP_ADDR+":"+IP_PORT+"/websocket/"+SID+"/"+secret


def buildControlCommandMessage(command, message, metadata):
    global SID,secret
    result = {
        'sid' : SID,
        'secret' : secret,
        'command' : command,
        'message' : message,
        'metadata' : metadata,
        'timestamp' : int(round(time.time()*1000))
    }

    return json.dumps(result)




def messageTick():
    wsConnect.send("wdnmd!")



def on_open(wsapp):
    print("on_open")


def on_message(wsapp, data):
    print("on_message", data, sep=",")
    receivedCommand = json.loads(data)
    match receivedCommand["command"]:
        case "SLAVE_START_RECORDING":
            return start_recording()
        case "SLAVE_STOP_RECORDING":
            return stop_recording()
        case "OBSERVER_STATUS":
            return print("handle OBSERVER_STATUS")


def on_error(wsapp, e):
    print("on_error", e, sep=",")


def on_close(wsapp, close_status_code, close_reason):
    print("on_close", close_status_code, close_reason, sep=",")
    wsBuildConnect()


def start_recording():
    print("start recording")
    obs.obs_frontend_recording_start()


def stop_recording():
    print("stop recording")

    obs.obs_frontend_recording_stop()

def sendMessageToCamera(currentSceneName):

    targetObSid = scene_camera_dictionary[currentSceneName]
    if (targetObSid!=None):
        metadata = {'sid':targetObSid}
        msg = buildControlCommandMessage("OBSERVER_STATUS",None,metadata);
        wsConnect.send(msg)

def on_event(event):
    if event == obs.OBS_FRONTEND_EVENT_SCENE_CHANGED:
        currentScene = obs.obs_frontend_get_current_scene();
        currentSceneName = obs.obs_source_get_name(currentScene)
        obs.obs_source_release(currentScene)
        

        sendMessageToCamera(currentSceneName)


def wsBuildConnect():
    global wsConnect
    wsConnect = websocket.WebSocketApp(buildFullUri(),
                                        on_open=on_open,
                                        on_message=on_message,
                                        on_error=on_error, on_close=on_close)
    wst = threading.Thread(target=wsConnect.run_forever)
    wst.daemon = True
    wst.start()



def script_description():
    return "This is a test python script"


def script_load(settings):
    global wsConnect
    print("script_load")
    obs.obs_frontend_add_event_callback(on_event)
    wsBuildConnect()


    # obs.timer_add(messageTick, 1000)


def script_properties():
    props = obs.obs_properties_create()
    return props


def script_unload():
    print("script_unload")
    if (wsConnect != None):
        wsConnect.close()
