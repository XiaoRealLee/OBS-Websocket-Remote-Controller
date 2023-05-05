package site.ps2cpc.obs_websocket_remote_controller.websocket;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import site.ps2cpc.obs_websocket_remote_controller.dto.CommandHandleResult;
import site.ps2cpc.obs_websocket_remote_controller.dto.config.OBSSetting;
import site.ps2cpc.obs_websocket_remote_controller.service.CommandHandleService;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
@Service
@ServerEndpoint("/websocket/{sid}/{secret}")
public class WebSocketServer {

    private static final AtomicInteger onlineCount = new AtomicInteger(0);

    //    private static CopyOnWriteArraySet<WebSocketServer> webSocketServers
//            = new CopyOnWriteArraySet<>();
    private static final ConcurrentHashMap<String, Set<WebSocketServer>> websocketMap = new ConcurrentHashMap<>();


    private static ConfigurableApplicationContext context;

    private Session session;

    private String sid;


    public WebSocketServer() {
    }

    public static void sendInfo(String message, @PathParam("sid") String sid) {
        log.info("推送消息到窗口" + sid + "，推送内容:" + message);

//        for (Set<WebSocketServer> sessionSet : websocketMap.values()) {
//            try {
//                //这里可以设定只推送给这个sid的，为null则全部推送
//                if (sid == null) {
////                    item.sendMessage(message);
//                } else if (sessionSet.sid.equals(sid)) {
//                    sessionSet.sendMessage(message);
//                }
//            } catch (IOException e) {
//                continue;
//            }
//        }

        if (!websocketMap.containsKey(sid)) {
            return;
        }
        Set<WebSocketServer> webSocketServers = websocketMap.get(sid);
        webSocketServers.forEach(server -> {
            try {
                server.sendMessage(message);
            } catch (IOException e) {
                log.error(String.format("Send Message failed: %s. %s, %s", e.getMessage(), sid, message));
            }
        });


    }

    public static void setApplicationContext(ConfigurableApplicationContext configurableApplicationContext) {
        context = configurableApplicationContext;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid, @PathParam("secret") String secret) {
        if (!verifySid(sid, secret)) {
            try {
                session.getBasicRemote().sendText("SID与密钥不匹配");
                session.close();
                return;
            } catch (IOException e) {
                log.error("websocket IO Exception:" + e.getMessage());
            }
        }

        this.session = session;
        Set<WebSocketServer> webSocketServers = websocketMap.get(sid);
        if (webSocketServers == null) {
            webSocketServers = new HashSet<>();
            websocketMap.put(sid, webSocketServers);
        }
        webSocketServers.add(this);
        this.sid = sid;
        addOnlineCount();

        log.info("有新窗口开始监听:" + sid + ",当前在线人数为:" + getOnlineCount());


    }

    private boolean verifySid(String sid, String secret) {
        OBSSetting obsSetting = context.getBean(OBSSetting.class);
        if (obsSetting.getSlave().containsKey(sid)) {
            return StringUtils.equals(secret, obsSetting.getSlave().get(sid));
        }
        return StringUtils.equals(sid, obsSetting.getAdmin().getSid()) && StringUtils.equals(secret, obsSetting.getAdmin().getSecret());
    }

    @OnClose

    public void onClose() {
        Set<WebSocketServer> webSocketServers = websocketMap.get(this.sid);
        if (webSocketServers != null) {
            webSocketServers.remove(this);
        }
        subOnlineCount();
        log.info("释放的sid为：" + sid);
        log.info("有一连接关闭！当前在线人数为" + getOnlineCount());

    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("收到来自窗口" + sid + "的信息:" + message);

        CommandHandleService commandHandleService = context.getBean(CommandHandleService.class);
//        OBSSetting obsSetting = context.getBean(OBSSetting.class);

        CommandHandleResult commandHandleResult = commandHandleService.handleMessage(message);
        sendCommandToSid(commandHandleResult);


        //这里应该进行消息的转换和处理。
//        //群发消息
//        for (WebSocketServer item : webSocketServers) {
//            try {
//                item.sendMessage(message);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }


    private void sendCommandToSid(CommandHandleResult commandHandleResult) {
        if (commandHandleResult == null) {
            log.info("命令为空");
            return;
        }
        if (commandHandleResult.getSidMessageMap() != null) {
            commandHandleResult.getSidMessageMap().forEach((targetSid, message) -> {
                if (websocketMap.containsKey(targetSid)) {

                    WebSocketServer.sendInfo(message, targetSid);
                }
            });
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("发生错误");
        error.printStackTrace();
    }

    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }


    public static synchronized int getOnlineCount() {
        return onlineCount.get();
    }

    public static synchronized void addOnlineCount() {
        WebSocketServer.onlineCount.incrementAndGet();
    }

    public static synchronized void subOnlineCount() {
        WebSocketServer.onlineCount.decrementAndGet();
    }

}
