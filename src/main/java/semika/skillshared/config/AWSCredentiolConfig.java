package semika.skillshared.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AwsConfigProperties.class)
public class AWSCredentiolConfig {

    @Autowired
    private AwsConfigProperties awsConfigProperties;

    @Bean
    public MosaicAWSCredentialProvier credentialProvier(){
        return new MosaicAWSCredentialProvier(awsConfigProperties);
    }
}
