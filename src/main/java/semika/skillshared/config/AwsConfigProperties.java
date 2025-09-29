package semika.skillshared.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aws")
public class AwsConfigProperties {

    private String accessKey;

    private String secretKay;

    private CognitoPool cognitoPool = new CognitoPool();

    @Data
    public static class CognitoPool {
        private String poolName;
        private String poolId;
        private String clientId;
        private String clientSecret;
    }
}
