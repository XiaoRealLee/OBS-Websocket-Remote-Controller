# import obspython as obs
from obs_scripting import obspython as obs
from random import randint
import requests
import json
import re
import websocket
import asyncio
import time
import threading

IP_ADDR = "xm1.dc.sunderer.games"
IP_PORT = "9961"
SID = "ob2"
secret = "123456"

wsConnect = None


def buildFullUri():
    return "ws://" + IP_ADDR + ":" + IP_PORT + "/websocket/" + SID + "/" + secret


def buildControlCommandMessage(command, message, metadata):
    global SID, secret
    result = {
        'sid': SID,
        'secret': secret,
        'command': command,
        'message': message,
        'metadata': metadata,
        'timestamp': int(round(time.time() * 1000))
    }

    return json.dumps(result)


def messageTick():
    wsConnect.send("wdnmd!")


def on_open(wsapp):
    print("on_open")


def on_message(wsapp, data):
    print("on_message", data, sep=",")
    receivedCommand = json.loads(data)
    print(receivedCommand)
    match receivedCommand["command"]:
        case "SLAVE_START_RECORDING":
            return start_recording()
        case "SLAVE_STOP_RECORDING":
            return stop_recording()


def on_error(wsapp, e):
    print("on_error", e, sep=",")


def on_close(wsapp, close_status_code, close_reason):
    print("on_close", close_status_code, close_reason, sep=",")


def start_recording():
    print("start recording")
    obs.obs_frontend_recording_start()


def stop_recording():
    print("stop recording")

    obs.obs_frontend_recording_stop()


def script_description():
    return "This is a test python script"


def script_load(settings):
    global wsConnect
    print("script_load")

    wsConnect = websocket.WebSocketApp(buildFullUri(),
                                       on_open=on_open,
                                       on_message=on_message,
                                       on_error=on_error, on_close=on_close)
    wst = threading.Thread(target=wsConnect.run_forever)
    wst.daemon = True
    wst.start()

    # obs.timer_add(messageTick, 1000)


def script_properties():
    props = obs.obs_properties_create()
    return props


def script_unload():
    print("script_unload")
    if (wsConnect != None):
        wsConnect.close()
