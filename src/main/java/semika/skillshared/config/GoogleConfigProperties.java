package semika.skillshared.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "google")
public class GoogleConfigProperties {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String grantType;
    private String tokenEndPoint;
}
