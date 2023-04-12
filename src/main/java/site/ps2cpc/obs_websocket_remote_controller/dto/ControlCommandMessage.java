package site.ps2cpc.obs_websocket_remote_controller.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import site.ps2cpc.obs_websocket_remote_controller.enums.CommandTypeEnum;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class ControlCommandMessage {

    private String sid;
    private String secret;
    private CommandTypeEnum commandType;
    private List<String> sidList;
    private Map<String,String> metadata;
    private Date timestamp;
}
