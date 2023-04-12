package site.ps2cpc.obs_websocket_remote_controller.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.ps2cpc.obs_websocket_remote_controller.dto.CommandHandleResult;
import site.ps2cpc.obs_websocket_remote_controller.dto.ControlCommandMessage;
import site.ps2cpc.obs_websocket_remote_controller.dto.SlaveControlMessage;
import site.ps2cpc.obs_websocket_remote_controller.dto.config.OBSSetting;
import site.ps2cpc.obs_websocket_remote_controller.enums.CommandType;

import java.util.*;

@Service
@Slf4j
public class CommandHandleService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private OBSSetting obsSetting;

    public CommandHandleService(@Autowired OBSSetting obsSetting) {
        this.obsSetting = obsSetting;
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

    @Data
    @Accessors(chain = true)
    private class VerifyResult {
        private boolean isValid;
        private boolean isAdminCommand;

    }
}
