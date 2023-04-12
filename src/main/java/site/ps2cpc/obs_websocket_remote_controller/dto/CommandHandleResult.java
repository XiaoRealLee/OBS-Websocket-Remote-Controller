package site.ps2cpc.obs_websocket_remote_controller.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
public class CommandHandleResult {

    Map<String,String> sidMessageMap;
}
