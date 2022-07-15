package semika.skillshared.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import semika.skillshared.config.AwsConfigProperties;
import semika.skillshared.model.request.MosaicSignupRequest;
import semika.skillshared.model.response.SignupResponse;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserSignupService implements UserSignup {

    @Autowired
    private AwsCredentialsProvider mosaicAWSCredentialProvier;

    @Autowired
    private AwsConfigProperties awsConfigProperties;

    @Override
    public SignupResponse createPool(MosaicSignupRequest mosaicSignupRequest) {

        CognitoIdentityProviderClient cognitoClient = null;
        String poolId = null;

        try {
            String userPoolName = "semika-test-pool";
            cognitoClient = CognitoIdentityProviderClient.builder()
                    .region(Region.AP_SOUTHEAST_1)
                    .credentialsProvider(mosaicAWSCredentialProvier)
                    .build();

            CreateUserPoolResponse response = cognitoClient.createUserPool(
                    CreateUserPoolRequest.builder()
                            .poolName(userPoolName)
                            .build()
            );
            poolId = response.userPool().id();
            System.out.println("User pool ID: " + poolId);

        } catch (CognitoIdentityProviderException e){
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        } finally {
            cognitoClient.close();
        }

        return SignupResponse.builder().message("new pool id : " + poolId).build();
    }

    public void createNewUser() {

        String firstName = "Semika"; // Input an unique username for the UserPool
        String lastName = "Siriwardana"; // Input the user phone number for the user Attribute
        String userPoolId = "ap-southeast-1_0KzqzRNXe"; // Input the UserPool Id, e.g. us-east-1_xxxxxxxx
        String password = "Lasanthi#101"; // Input the temporary password for the user
        String email = "semika.siriwardana@gmail.com"; // Input the email for the user attribute

        CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.builder()
                .region(Region.AP_SOUTHEAST_1)
                .credentialsProvider(mosaicAWSCredentialProvier)
                .build();

        try {
            AttributeType userAttrs = AttributeType.builder()
                    .name("email").value(email)
                    .name("given_name").value(firstName)
                    .name("family_name").value(lastName)
                    .name("preferred_username").value("semikas")
                    .name("phone_number").value("+94713258253")
                    .build();

            AdminCreateUserRequest userRequest = AdminCreateUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(email)
                    .temporaryPassword(password)
                    .userAttributes(userAttrs)
                    .messageAction("SUPPRESS")
                    .build() ;

            AdminCreateUserResponse response = cognitoClient.adminCreateUser(userRequest);
            System.out.println("User " + response.user().username() + "is created. Status: " + response.user().userStatus());

        } catch (CognitoIdentityProviderException e){
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

    public SignUpResponse signup(MosaicSignupRequest mosaicSignupRequest) {
        String clientId = awsConfigProperties.getCognitoPool().getClientId();
        String secretKey = awsConfigProperties.getCognitoPool().getClientSecret();

        CognitoIdentityProviderClient identityProviderClient = CognitoIdentityProviderClient.builder()
                .region(Region.AP_SOUTHEAST_1)
                .credentialsProvider(mosaicAWSCredentialProvier)
                .build();

        AttributeType email = getAttributeType("email", mosaicSignupRequest.getEmail());
        AttributeType given_name = getAttributeType("given_name", mosaicSignupRequest.getFirstName());
        AttributeType family_name = getAttributeType("family_name", mosaicSignupRequest.getLastName());
        AttributeType preferred_username = getAttributeType("preferred_username", mosaicSignupRequest.getUserName());
        AttributeType phone_number = getAttributeType("phone_number", mosaicSignupRequest.getPhoneNumber());

        List<AttributeType> attrs = new ArrayList<>();
        attrs.add(email);
        attrs.add(given_name);
        attrs.add(family_name);
        attrs.add(preferred_username);
        attrs.add(phone_number);

        try {
            String secretVal = calculateSecretHash(clientId, secretKey, mosaicSignupRequest.getEmail());
            SignUpRequest signUpRequest = SignUpRequest.builder()
                    .userAttributes(attrs)
                    .username(mosaicSignupRequest.getEmail())
                    .clientId(clientId)
                    .password(mosaicSignupRequest.getPassword())
                    .secretHash(secretVal)
                    .build();

            return identityProviderClient.signUp(signUpRequest);

        } catch(CognitoIdentityProviderException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        return null;
    }

    private AttributeType getAttributeType(String name, String value) {
        return AttributeType.builder()
                .name(name)
                .value(value)
                .build();
    }

    private String calculateSecretHash(String userPoolClientId,
                                       String userPoolClientSecret,
                                       String userName) throws NoSuchAlgorithmException, InvalidKeyException {
        final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

        SecretKeySpec signingKey = new SecretKeySpec(
                userPoolClientSecret.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256_ALGORITHM);

        Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
        mac.init(signingKey);
        mac.update(userName.getBytes(StandardCharsets.UTF_8));
        byte[] rawHmac = mac.doFinal(userPoolClientId.getBytes(StandardCharsets.UTF_8));
        return java.util.Base64.getEncoder().encodeToString(rawHmac);
    }

    public ConfirmSignUpResponse confirmSignup(String code) throws NoSuchAlgorithmException, InvalidKeyException {
        String clientId = awsConfigProperties.getCognitoPool().getClientId();
        String secretKey = awsConfigProperties.getCognitoPool().getClientSecret();
        String secretVal = calculateSecretHash(clientId,
                secretKey,
                "semika.siriwardana@gmail.com");

        ConfirmSignUpRequest confirmSignUpRequest = ConfirmSignUpRequest.builder()
                .username("semika.siriwardana@gmail.com")
                .clientId(clientId)
                .secretHash(secretVal)
                .confirmationCode(code)
                .build();

        CognitoIdentityProviderClient identityProviderClient = CognitoIdentityProviderClient.builder()
                .region(Region.AP_SOUTHEAST_1)
                .credentialsProvider(mosaicAWSCredentialProvier)
                .build();
        return identityProviderClient.confirmSignUp(confirmSignUpRequest);
    }

    public void sendSms(String phoneNumber) {
        final String usage = "\n" +
                "Usage: " +
                "   <message> <phoneNumber>\n\n" +
                "Where:\n" +
                "   message - The message text to send.\n\n" +
                "   phoneNumber - The mobile phone number to which a message is sent (for example, +1XXX5550100). \n\n";

        String message = "Hi, I am from AWS";

        SnsClient snsClient = SnsClient.builder()
                .region(Region.AP_SOUTHEAST_1)
                .credentialsProvider(mosaicAWSCredentialProvier)
                .build();
        pubTextSMS(snsClient, message, phoneNumber);
    }

    //snippet-start:[sns.java2.PublishTextSMS.main]
    public static void pubTextSMS(SnsClient snsClient, String message, String phoneNumber) {
        try {
            Map<String, MessageAttributeValue> attributes = new HashMap();

            attributes.put("AWS.SNS.SMS.SMSType", MessageAttributeValue
                    .builder().stringValue("Transactional")
                    .dataType("String").build());

            PublishRequest request = PublishRequest.builder()
                    .message(message)
                    .phoneNumber(phoneNumber)
                    .messageAttributes(attributes)
                    .build();

            PublishResponse result = snsClient.publish(request);
            System.out.println(result.messageId() + " Message sent. Status was " + result.sdkHttpResponse().statusCode());

        } catch (SnsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }
}
