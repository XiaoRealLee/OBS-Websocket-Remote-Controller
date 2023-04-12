package site.ps2cpc.obs_websocket_remote_controller.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import site.ps2cpc.obs_websocket_remote_controller.enums.CommandEnum;
import site.ps2cpc.obs_websocket_remote_controller.enums.CommandType;

import java.util.Date;
import java.util.Map;

@Data
@Accessors(chain = true)
public class SlaveControlMessage {
    private String messageSource;
    private CommandType commandType;
    private CommandEnum command;
    private String message;
    private Map<String,String> metadata;
    private Date timestamp;

}
