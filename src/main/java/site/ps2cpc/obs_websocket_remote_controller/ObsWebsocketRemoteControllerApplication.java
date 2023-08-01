package site.ps2cpc.obs_websocket_remote_controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import site.ps2cpc.obs_websocket_remote_controller.websocket.WebSocketServer;

@SpringBootApplication
public class ObsWebsocketRemoteControllerApplication {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(ObsWebsocketRemoteControllerApplication.class);
        ConfigurableApplicationContext configurableApplicationContext = springApplication.run(args);
        WebSocketServer.setApplicationContext(configurableApplicationContext);
//        SpringApplication.run(ObsWebsocketRemoteControllerApplication.class, args);
    }

}
