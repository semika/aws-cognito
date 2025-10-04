package semika.skillshared.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "apple")
public class AppleConfigProperties {
    private String clientId;
    //private String clientSecret;
    private String redirectUri;
    private String grantType;
    private String tokenEndPoint;
    private String signinKeyName;
    private String signinKeyId;
    private String signinKeyContent;
    private String teamId;
    private String frontEndRedirectUrl;
}
