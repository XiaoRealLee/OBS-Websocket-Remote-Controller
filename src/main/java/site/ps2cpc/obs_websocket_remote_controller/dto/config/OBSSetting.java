package site.ps2cpc.obs_websocket_remote_controller.dto.config;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "obs")
@Data
@Accessors(chain = true)
public class OBSSetting {

    private OBSClientInfo admin;

    private Map<String,String> slave;
}
