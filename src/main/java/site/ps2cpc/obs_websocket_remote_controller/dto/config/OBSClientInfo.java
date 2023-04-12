package site.ps2cpc.obs_websocket_remote_controller.dto.config;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class OBSClientInfo {
    private String sid;
    private String secret;
}
