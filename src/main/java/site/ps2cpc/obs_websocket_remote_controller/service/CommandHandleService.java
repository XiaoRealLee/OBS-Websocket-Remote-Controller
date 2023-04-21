package site.ps2cpc.obs_websocket_remote_controller.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Service;
import site.ps2cpc.obs_websocket_remote_controller.dto.CommandHandleResult;
import site.ps2cpc.obs_websocket_remote_controller.dto.ControlCommandMessage;
import site.ps2cpc.obs_websocket_remote_controller.dto.SlaveControlMessage;
import site.ps2cpc.obs_websocket_remote_controller.dto.config.OBSSetting;
import site.ps2cpc.obs_websocket_remote_controller.enums.CommandType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class CommandHandleService implements DisposableBean {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private OBSSetting obsSetting;
    private FileWriter fileWriter;

    public CommandHandleService(@Autowired OBSSetting obsSetting) throws IOException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        this.obsSetting = obsSetting;
        ApplicationHome home = new ApplicationHome(getClass());
        File jarFile = home.getSource();
        String commandLogFilePath = String.format("%s%s%s-command-log.txt",jarFile.getParent(),File.separator,simpleDateFormat.format(new Date()));
        File commandLogFile = new File(commandLogFilePath);
        if (commandLogFile.exists()){
            commandLogFile.delete();
        }
        commandLogFile.getParentFile().mkdirs();
        commandLogFile.getParentFile().createNewFile();
        log.debug(String.valueOf(commandLogFile.exists()));


        this.fileWriter = new FileWriter(commandLogFile);
    }

    public CommandHandleResult handleMessage(String message) {
        ControlCommandMessage controlCommandMessage;
        try {
            controlCommandMessage = objectMapper.readValue(message, ControlCommandMessage.class);
            VerifyResult verifyResult = verifyCommand(controlCommandMessage);
            if (verifyResult == null || (!verifyResult.isValid())) {
                log.warn("收到验证失败的消息：" + message);
                return null;
            }
            if (verifyResult.isAdminCommand()) {
                return handleAdminCommand(controlCommandMessage);
            } else {
                return handleSlaveCommand(controlCommandMessage);
            }

        } catch (JsonProcessingException e) {
            log.error("无法解析的命令：" + message);
        }
        return null;
    }

    private CommandHandleResult handleSlaveCommand(ControlCommandMessage controlCommandMessage) {

        return null;
    }

    private CommandHandleResult handleAdminCommand(ControlCommandMessage controlCommandMessage) {
        switch (controlCommandMessage.getCommand()) {
            case OBSERVER_STATUS -> {
                SlaveControlMessage slaveControlMessage = new SlaveControlMessage()
                        .setMessageSource(controlCommandMessage.getSid())
                        .setCommandType(CommandType.COMMAND)
                        .setCommand(controlCommandMessage.getCommand())
                        .setMessageSource(controlCommandMessage.getSid())
                        .setTimestamp(new Date())
                        .setMetadata(controlCommandMessage.getMetadata());
                List<String> allSidList = buildAllSlaveList();


                Map<String, String> metadata = controlCommandMessage.getMetadata();

                if (metadata!=null){
                    if (metadata.containsKey("msg")) {
                        try {
                            this.fileWriter.write(metadata.get("msg"));
                            this.fileWriter.write("\n");
                            this.fileWriter.flush();

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                return sendSlaveControlMessageToSid(allSidList, slaveControlMessage);
            }
            default -> {
                SlaveControlMessage slaveControlMessage = new SlaveControlMessage()
                        .setMessageSource(controlCommandMessage.getSid())
                        .setCommandType(CommandType.COMMAND)
                        .setCommand(controlCommandMessage.getCommand())
                        .setMessageSource(controlCommandMessage.getSid())
                        .setTimestamp(new Date())
                        .setMetadata(controlCommandMessage.getMetadata());


                Map<String, String> metadata = controlCommandMessage.getMetadata();

                if (metadata!=null){
                    if (metadata.containsKey("msg")) {
                        try {
                            this.fileWriter.write(metadata.get("msg"));
                            this.fileWriter.write("\n");
                            this.fileWriter.flush();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                return sendSlaveControlMessageToSid(controlCommandMessage.getSidList(), slaveControlMessage);
            }
        }

    }

    private List<String> buildAllSlaveList() {
        List<String> list = new ArrayList<>();
        for (String s : obsSetting.getSlave().keySet()) {
            list.add(s);
        }
        return list;
    }

    private CommandHandleResult sendSlaveControlMessageToSid(List<String> sidList, SlaveControlMessage slaveControlMessage) {

        String commandStr;
        try {
            commandStr = objectMapper.writeValueAsString(slaveControlMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        Map<String, String> sidCommandMap = null;
        if (sidList!=null && sidList.size()>0) {
            sidCommandMap = new HashMap<>();
            for (String item : sidList) {
                sidCommandMap.put(item, commandStr);
            }
        }
        return new CommandHandleResult().setSidMessageMap(sidCommandMap).setSenderSID(slaveControlMessage.getMessageSource());

    }

    private VerifyResult verifyCommand(ControlCommandMessage controlCommandMessage) {
        if (StringUtils.equals(controlCommandMessage.getSid(), obsSetting.getAdmin().getSid())) {
            //是管理员机
            boolean isValid = StringUtils.equals(controlCommandMessage.getSecret(), obsSetting.getAdmin().getSecret());
            return new VerifyResult().setValid(isValid).setAdminCommand(true);
        } else {
            if (obsSetting.getSlave().containsKey(controlCommandMessage.getSid())) {
                String secret = obsSetting.getSlave().get(controlCommandMessage.getSid());
                boolean isValid = StringUtils.equals(secret, controlCommandMessage.getSecret());
                return new VerifyResult().setValid(isValid).setAdminCommand(true);
            }
        }
        return null;
    }

    @Override
    public void destroy() throws Exception {

        this.fileWriter.flush();
        this.fileWriter.close();
    }

    @Data
    @Accessors(chain = true)
    private class VerifyResult {
        private boolean isValid;
        private boolean isAdminCommand;

    }
}
