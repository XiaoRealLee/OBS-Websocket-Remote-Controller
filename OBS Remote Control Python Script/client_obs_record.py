import threading
import time

import websocket
# from websockets.sync.client import connect
from websockets import connect
# from obs_scripting import obspython as obs
# import obspython as obs
import json
import asyncio


class ClientObsRecord:
    IP_ADDR = "127.0.0.1"
    IP_PORT = "8000"
    SID = "ob2"
    secret = "123456"

    def __init__(self):
        self.url = "ws://" + self.IP_ADDR + ":" + self.IP_PORT + "/websocket/" + self.SID + "/" + self.secret
        self.ws = None

    # async def main(self):
    #     async with connect("ws://localhost:8000/ws/chat/lobby/") as websocket:
    #         # self.ws = websocket
    #         # await self.ws.send(json.dumps({"message": "%s已连接" % self.SID, "user": self.SID, "user_type": "OB"}))
    #         await websocket.send(json.dumps({"message": "%s已连接" % self.SID, "user": self.SID, "user_type": "OB"}))
    #         print("websockets已连接")
    #         # for i in range(10):
    #         #     self.rev_message_from_server()
    #         while websocket.open:
    #             message = await self.rev_message_from_server(websocket)
    #             if message:
    #                 pass
    #                 # self.test()
    #                 # message = self.rev_message_from_server()
    #                 # message = {"command": "SLAVE_START_RECORDING"}
    #                 # match message["command"]:
    #                 #     case "SLAVE_START_RECORDING":
    #                 #         return self.start_recording()
    #                 #     case "SLAVE_STOP_RECORDING":
    #                 #         return self.stop_recording()
    #             else:
    #                 pass
    #             time.sleep(1)
    def main(self):
        websocket.WebSocketApp("ws://localhost:8000/ws/chat/lobby/",
                               on_message=self.on_message).run_forever()

    def build_control_command_message(self, command, message, metadata):
        result = {
            'sid': self.SID,
            'secret': self.secret,
            'command': command,
            'message': message,
            'metadata': metadata,
            'timestamp': int(round(time.time() * 1000))
        }

        return json.dumps(result)

    def send_message_to_sever(self, message):
        message = {"message": message, "user": self.SID, "user_type": "OB"}
        message = json.dumps(message)
        self.ws.send(message)

    def on_message(self, wsapp, message):
        message = json.loads(message)
        print("接收到服务端消息:%s" % message['message'])
        return message

    def rev_message_from_server(self, data):
        # message = await self.ws.recv()
        # message = websocket.recv()
        message = json.loads(data)
        print("接收到服务端消息:%s" % message['message'])
        return message

    # def test(self):
    #     scenesource = obs.obs_frontend_get_current_scene()
    #
    #     current_scene = obs.obs_source_get_name(scenesource)
    #     print(current_scene)

    # def start_recording(self, ):
    #     print("start recording")
    #     obs.obs_frontend_recording_start()
    #
    # def stop_recording(self, ):
    #     print("stop recording")
    #
    #     obs.obs_frontend_recording_stop()
    #
    #
    # def script_load(self, settings):
    #     global wsConnect
    #     print("script_load")
    #
    #     # obs.timer_add(messageTick, 1000)
    #
    # def script_properties(self, ):
    #     props = obs.obs_properties_create()
    #     return props


def script_load(settings):
    test = ClientObsRecord()
    # test.main()
    a = threading.Thread(target=test.main())
    a.daemon = True
    a.start()
    # a = threading.Thread(target=test.main())
    # a.daemon = True
    # a.start()

    # asyncio.run(test.main())
    # obs.obs_frontend_add_event_callback(on_scene_changed_event)
    # obs.obs_frontend_add_event_callback(on_preview_scene_changed_event)
    # scenesource = obs.obs_frontend_get_current_scene()
    #
    # current_scene = obs.obs_source_get_name(scenesource)
    # print(current_scene)
    #
    # obs.obs_source_release(scenesource)


# if __name__ == '__main__':
#     test = ClientObsRecord()
#     # test.main()
#     a = threading.Thread(target=test.main())
#     a.daemon = True
#     a.start()
