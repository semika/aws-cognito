package semika.skillshared.config;

import software.amazon.awssdk.auth.credentials.AwsCredentials;

public class MosaicAWSCredential implements AwsCredentials {

    private AwsConfigProperties awsConfigProperties;

    public MosaicAWSCredential(AwsConfigProperties awsConfigProperties) {
        this.awsConfigProperties = awsConfigProperties;
    }

    @Override
    public String accessKeyId() {
        return awsConfigProperties.getAccessKey();
    }

    @Override
    public String secretAccessKey() {
        return awsConfigProperties.getSecretKay();
    }
}
