package site.ps2cpc.obs_websocket_remote_controller.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import site.ps2cpc.obs_websocket_remote_controller.enums.CommandTypeEnum;

import java.util.Date;
import java.util.Map;

@Data
@Accessors(chain = true)
public class SlaveControlMessage {
    private String messageSource;
    private CommandTypeEnum commandType;
    private Map<String,String> metadata;
    private Date timestamp;

}
