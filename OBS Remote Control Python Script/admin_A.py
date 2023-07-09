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
SID = "admin"
secret = "6657"

miniview_setting = {
    "volume": 0,
    "width": 764,
    "height": 430,
    "position": {
        "x": 1160,
        "y": 650
    }
}
current_scene_miniview_name = None
current_scene = None
latest_miniview_command = None

isKeepMiniview = False
keep_miniview_command = None

current_playing_miniview_source_name = None

scene_camera_dictionary = {
    'TestOB1': ['ob1'],
    'TestOB2': ['ob2'],
    'TestOB3': ['ob3'],
    'Empty': []
}

wsConnect = None


def buildFullUri():
    return "ws://" + IP_ADDR + ":" + IP_PORT + "/websocket/" + SID + "/" + secret


def buildControlCommandMessage(command, sidList, message, metadata):
    global SID, secret
    result = {
        'sid': SID,
        'secret': secret,
        'command': command,
        'sidList': sidList,
        'message': message,
        'metadata': metadata,
        'timestamp': int(round(time.time() * 1000))
    }

    return json.dumps(result)


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
        case "MINI_VIEW_ADD":
            return update_miniview(receivedCommand)
        case "MINI_VIEW_CLEAR":
            return clean_miniview()


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


def sendMessageToServer(currentSceneName, isPreview):
    commandName = "OBSERVER_STATUS_PREVIEW" if isPreview else "OBSERVER_STATUS_PLAYING"

    targetObSid = scene_camera_dictionary.get(currentSceneName, None)
    metadata = {'msg': str(time.localtime(time.time())) + commandName + " all clear" if targetObSid == None else str(
        time.localtime(time.time())) + commandName + " " + str(targetObSid)}

    msg = buildControlCommandMessage(commandName, [] if targetObSid == None else targetObSid, None, metadata)
    print("send message to server: ", msg)
    wsConnect.send(msg)


def add_media_source(scene, group_source_item, source_name, stream_uri, width, height, pos_x, pos_y, order_down_count,
                     volume):
    settings = obs.obs_data_create()
    obs.obs_data_set_bool(settings, "is_local_file", False)
    obs.obs_data_set_bool(
        settings, "restart_on_activate", False
    )
    obs.obs_data_set_bool(
        settings, "is_hw_decoding", True
    )

    obs.obs_data_set_bool(
        settings, "close_when_inactive", False
    )

    obs.obs_data_set_string(settings, "input", stream_uri)

    # obs.obs_data_set_bool(settings, "is_local_file", True)
    # obs.obs_data_set_string(
    #     # settings, "input", stream_uri
    #     settings, "local_file", "C:\\Users\\sduli\\Downloads\\Video\\Walter White in Mario Kart Wii.mkv"
    # )v
    # obs.obs_data_set_bool(
    #     settings, "close_when_inactive", False
    # )

    source = obs.obs_source_create(
        "ffmpeg_source", source_name, settings, None)
    obs.obs_source_set_volume(source, volume)
    pos = obs.vec2()
    pos.x = pos_x
    pos.y = pos_y

    scale = obs.vec2()
    scale.x = width / 1920
    scale.y = height / 1080

    print(scale.x)
    print(scale.y)

    scene_item = obs.obs_scene_add(scene, source)
    if group_source_item:
        obs.obs_sceneitem_group_add_item(group_source_item, scene_item)
    obs.obs_sceneitem_set_pos(scene_item, pos)
    obs.obs_sceneitem_set_scale(scene_item, scale)
    if order_down_count > 0:
        for x in range(order_down_count):
            obs.obs_sceneitem_set_order(scene_item, obs.OBS_ORDER_MOVE_DOWN)

    obs.obs_source_release(source)
    obs.obs_data_release(settings)


def update_miniview(receivedCommand):
    global isKeepMiniview, latest_miniview_command

    # 收到新命令后，首先删除所有老命令的多余视频源
    if receivedCommand == latest_miniview_command:
        pass
    else:
        delete_other_preview_miniview()

    latest_miniview_command = receivedCommand
    metadata = receivedCommand['metadata']
    isKeepMiniview = str("true") == metadata['keepMiniview']

    # 检查一下是否是keep miniview
    # 如果是，记录该命令，并将isKeepMiniview 设置为true

    currentPreviewSceneSource = obs.obs_frontend_get_current_preview_scene()
    currentPreviewScene = obs.obs_scene_from_source(currentPreviewSceneSource)
    currentPreviewSceneName = obs.obs_source_get_name(currentPreviewSceneSource)

    # 查找当前活动的 preview source，并添加新的  miniview
    newPreviewName = currentPreviewSceneName + "-" + metadata["obsObjectName"]

    check_exist = obs.obs_get_source_by_name(newPreviewName)
    obs.obs_source_release(check_exist)
    if check_exist is None:
        add_media_source(currentPreviewScene, None, newPreviewName,
                         metadata["streamUri"], miniview_setting["width"],
                         miniview_setting["height"], miniview_setting["position"]["x"],
                         miniview_setting["position"]["y"],
                         0, 0)
    # 立刻切换场景
    # obs.obs_frontend_set_current_scene(currentSceneSource)

    obs.obs_source_release(currentPreviewSceneSource)


def delete_other_preview_miniview():
    if latest_miniview_command is None:
        return
    currentScene = obs.obs_frontend_get_current_scene()
    currentSceneName = obs.obs_source_get_name(currentScene)

    obs.obs_source_release(currentScene)

    temp_scene_List = obs.obs_frontend_get_scenes()
    metadata = latest_miniview_command['metadata']
    temp_miniview_suffix = metadata['obsObjectName']

    for temp_scene in temp_scene_List:
        temp_scene_name = obs.obs_source_get_name(temp_scene)
        if temp_scene_name == currentSceneName:
            print(temp_scene_name + ": pass")
        else:

            delete_miniview_preview_name = temp_scene_name + "-" + temp_miniview_suffix
            delete_miniview_source = obs.obs_get_source_by_name(delete_miniview_preview_name)
            if delete_miniview_source:
                obs.obs_source_remove(delete_miniview_source)
                obs.obs_source_release(delete_miniview_source)
        obs.obs_source_release(temp_scene)

def delete_current_playing_miniview():
    global current_playing_miniview_source_name

    if current_playing_miniview_source_name is None:
        return

    delete_miniview_source = obs.obs_get_source_by_name(current_playing_miniview_source_name)
    if delete_miniview_source:
        obs.obs_source_remove(delete_miniview_source)
        obs.obs_source_release(delete_miniview_source)
    current_playing_miniview_source_name = None


def clean_miniview():
    global isKeepMiniview, latest_miniview_command
    delete_other_preview_miniview()
    delete_current_playing_miniview()

    isKeepMiniview = None
    latest_miniview_command = None


def on_scene_changed_event(event):
    global current_playing_miniview_source_name
    if event == obs.OBS_FRONTEND_EVENT_SCENE_CHANGED:
        print("OBS_FRONTEND_EVENT_SCENE_CHANGED triggered!")
        currentScene = obs.obs_frontend_get_current_scene()
        currentSceneName = obs.obs_source_get_name(currentScene)
        print("current scene: " + currentSceneName)

        obs.obs_source_release(currentScene)
        sendMessageToServer(currentSceneName, False)

        # 这里要删除所有上个指令插入的视频源，除了符合当前场景的那个
        delete_other_preview_miniview()
        # 删除上次正在播放的那个视频源
        # 删除之前还得检查一下是不是同一个

        if latest_miniview_command is not None:

            old_current_playing_miniview_source_name = current_playing_miniview_source_name
            new_current_playing_miniview_source_name = currentSceneName + "-" + latest_miniview_command['metadata'][
                'obsObjectName']
            if old_current_playing_miniview_source_name == new_current_playing_miniview_source_name:
                pass
            else:
                # 删除旧的
                if old_current_playing_miniview_source_name is not None:
                    delete_source = obs.obs_get_source_by_name(old_current_playing_miniview_source_name)
                    if delete_source:
                        obs.obs_source_remove(delete_source)
                        obs.obs_source_release(delete_source)
                current_playing_miniview_source_name = new_current_playing_miniview_source_name


def on_preview_scene_changed_event(event):
    if event == obs.OBS_FRONTEND_EVENT_PREVIEW_SCENE_CHANGED:

        print("OBS_FRONTEND_EVENT_PREVIEW_SCENE_CHANGED triggered!")
        currentPreviewScene = obs.obs_frontend_get_current_preview_scene()
        currentPreviewSceneName = obs.obs_source_get_name(currentPreviewScene)
        obs.obs_source_release(currentPreviewScene)

        sendMessageToServer(currentPreviewSceneName, True)

        if isKeepMiniview:
            print("isKeepView check:")
            print(isKeepMiniview)
            update_miniview(latest_miniview_command)


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
    global wsConnect, current_scene
    print("script_load")
    obs.obs_frontend_add_event_callback(on_scene_changed_event)
    obs.obs_frontend_add_event_callback(on_preview_scene_changed_event)
    scenesource = obs.obs_frontend_get_current_scene();

    current_scene = obs.obs_source_get_name(scenesource)
    print(current_scene)

    obs.obs_source_release(scenesource)
    wsBuildConnect()

    # obs.timer_add(messageTick, 1000)


def script_properties():
    props = obs.obs_properties_create()
    return props


def script_unload():
    global wsConnect
    print("script_unload")
    if current_scene_miniview_name:
        clean_miniview()
    if wsConnect is not None:
        wsConnect.close()
