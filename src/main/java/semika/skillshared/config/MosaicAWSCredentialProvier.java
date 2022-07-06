package semika.skillshared.config;

import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

public class MosaicAWSCredentialProvier implements AwsCredentialsProvider {

    //@Autowired
    private AwsConfigProperties awsConfigProperties;

    public MosaicAWSCredentialProvier(AwsConfigProperties awsConfigProperties) {
        this.awsConfigProperties = awsConfigProperties;
    }

    @Override
    public AwsCredentials resolveCredentials() {
        return new MosaicAWSCredential(awsConfigProperties);
    }
}
