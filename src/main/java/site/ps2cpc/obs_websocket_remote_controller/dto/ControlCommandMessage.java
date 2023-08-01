package site.ps2cpc.obs_websocket_remote_controller.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import site.ps2cpc.obs_websocket_remote_controller.enums.CommandEnum;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class ControlCommandMessage {

    private String sid;
    private String secret;
    private CommandEnum command;
    private String message;
    private List<String> sidList;
    private Map<String,String> metadata;
    private Date timestamp;
}
