package semika.skillshared.service;

import org.apache.tomcat.util.buf.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import semika.skillshared.config.AwsConfigProperties;
import semika.skillshared.model.request.MosaicResendConfirmationCodeRequest;
import semika.skillshared.model.request.MosaicSignupRequest;
import semika.skillshared.model.request.MosaicSingupConfirmRequest;
import semika.skillshared.model.request.UserDto;
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
            //String userPoolName = awsConfigProperties.getCognitoPool().getPoolName();
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

    public String createNewUser(UserDto userDto) {
        //String userPoolId = "ap-southeast-1_0KzqzRNXe"; // Input the UserPool Id, e.g. us-east-1_xxxxxxxx
        String userPoolId = awsConfigProperties.getCognitoPool().getPoolId();
        CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.builder()
                .region(Region.AP_SOUTHEAST_1)
                .credentialsProvider(mosaicAWSCredentialProvier)
                .build();

        try {
            AttributeType userAttrs = AttributeType.builder()
                    .name("email").value(userDto.getEmail())
                    .name("given_name").value(userDto.getFirstName())
                    .name("family_name").value(userDto.getLastName())
                    .name("preferred_username").value(userDto.getUserName())
                    .name("phone_number").value("+94713258253")
                    .build();

            AdminCreateUserRequest userRequest = AdminCreateUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(userDto.getEmail())
                    .temporaryPassword(userDto.getPassword())
                    .userAttributes(userAttrs)
                    .messageAction("SUPPRESS")
                    .build() ;

            AdminCreateUserResponse response = cognitoClient.adminCreateUser(userRequest);
            return ("User " + response.user().username()
                    + "is created. Status: "
                    + response.user().userStatus());

        } catch (CognitoIdentityProviderException e){
            System.err.println(e.awsErrorDetails().errorMessage());
            return "User Creation Failed " + e.awsErrorDetails().errorMessage();
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
        AttributeType federated_user = getAttributeType("profile", "Google");

        List<AttributeType> attrs = new ArrayList<>();
        attrs.add(email);
        attrs.add(given_name);
        attrs.add(family_name);
        attrs.add(preferred_username);
        attrs.add(phone_number);
        attrs.add(federated_user);

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

    public ConfirmSignUpResponse confirmSignup(MosaicSingupConfirmRequest mosaicSingupConfirmRequest)
            throws NoSuchAlgorithmException, InvalidKeyException {
        String clientId = awsConfigProperties.getCognitoPool().getClientId();
        String secretKey = awsConfigProperties.getCognitoPool().getClientSecret();
        String secretVal = calculateSecretHash(clientId,
                secretKey,
                mosaicSingupConfirmRequest.getEmail());

        ConfirmSignUpRequest confirmSignUpRequest = ConfirmSignUpRequest.builder()
                .username(mosaicSingupConfirmRequest.getEmail())
                .clientId(clientId)
                .secretHash(secretVal)
                .confirmationCode(mosaicSingupConfirmRequest.getCode())
                .build();

        CognitoIdentityProviderClient identityProviderClient = CognitoIdentityProviderClient.builder()
                .region(Region.AP_SOUTHEAST_1)
                .credentialsProvider(mosaicAWSCredentialProvier)
                .build();
        return identityProviderClient.confirmSignUp(confirmSignUpRequest);
    }
    public String resendConfirmationCode(MosaicResendConfirmationCodeRequest mosaicResendConfirmationCodeRequest) {
        try {
            CognitoIdentityProviderClient identityProviderClient = CognitoIdentityProviderClient.builder()
                    .region(Region.AP_SOUTHEAST_1)
                    .credentialsProvider(mosaicAWSCredentialProvier)
                    .build();
            String clientId = awsConfigProperties.getCognitoPool().getClientId();
            String secretKey = awsConfigProperties.getCognitoPool().getClientSecret();

            String secretVal = calculateSecretHash(clientId,
                    secretKey,
                    mosaicResendConfirmationCodeRequest.getEmail());

            ResendConfirmationCodeRequest codeRequest = ResendConfirmationCodeRequest.builder()
                    .clientId(clientId)
                    .secretHash(secretVal)
                    .username(mosaicResendConfirmationCodeRequest.getEmail()) // Need to provide email here.
                    .build();

            ResendConfirmationCodeResponse response = identityProviderClient.resendConfirmationCode(codeRequest);
            return ("Method of delivery is " + response.codeDeliveryDetails().deliveryMediumAsString());

        } catch (CognitoIdentityProviderException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            return e.awsErrorDetails().errorMessage();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return e.getMessage();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return e.getMessage();
        }
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

    public String loginWithCustomChallenge(String email) {

        final Map<String, String> authParameters = new HashMap<>();
        authParameters.put("USERNAME", email);
        authParameters.put("ChallengeName", "CUSTOM_CHALLENGE");

        CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.builder()
                .region(Region.AP_SOUTHEAST_1)
                .credentialsProvider(mosaicAWSCredentialProvier)
                .build();
        String clientId = awsConfigProperties.getCognitoPool().getClientId();
        String secretKey = awsConfigProperties.getCognitoPool().getClientSecret();

        // Client metadata to tell Lambda what stage this is
        Map<String, String> clientMetadata = new HashMap<>();
        clientMetadata.put("flow-type", "SSO");

        try {
            final InitiateAuthRequest initiateAuthRequest =
                    InitiateAuthRequest.builder()
                            .authFlow(AuthFlowType.CUSTOM_AUTH)
                            .clientId(clientId)
                            .authParameters(authParameters)
                            .clientMetadata(clientMetadata)
                            //.userContextData(UserContextDataType.builder().encodedData().build())
                            .build();

            final InitiateAuthResponse initiateAuthResponse =
                    cognitoClient.initiateAuth(initiateAuthRequest);

            System.out.println("Session: " + initiateAuthResponse.session());
            return initiateAuthResponse.session();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error");
        }
    }

    public String responseToAuthChallage(String email, String otp, String session) {

        CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.builder()
                .region(Region.AP_SOUTHEAST_1)
                .credentialsProvider(mosaicAWSCredentialProvier)
                .build();
        String clientId = awsConfigProperties.getCognitoPool().getClientId();
        String secretKey = awsConfigProperties.getCognitoPool().getClientSecret();

        // This will be the challenge parameters (like OTP answer, etc.)
        Map<String, String> challengeResponses = new HashMap<>();
        challengeResponses.put("USERNAME", email);
        challengeResponses.put("ANSWER", otp); // 👈 user’s input (e.g. OTP)

        // Client metadata to tell Lambda what stage this is
        Map<String, String> clientMetadata = new HashMap<>();
        clientMetadata.put("flowStage", "SENSITIVE_ACTION");

        RespondToAuthChallengeRequest challengeRequest = RespondToAuthChallengeRequest.builder()
                .clientId(clientId)
                .challengeName("CUSTOM_CHALLENGE") // must match challenge from Cognito
                .session(session)
                .challengeResponses(challengeResponses)
                .clientMetadata(clientMetadata)  // ✅ stage info here
                .build();

        RespondToAuthChallengeResponse challengeResponse =
                cognitoClient.respondToAuthChallenge(challengeRequest);

        System.out.println("Access Token: " + challengeResponse.authenticationResult().accessToken());
        System.out.println("Refresh Token: " + challengeResponse.authenticationResult().refreshToken());
        System.out.println("Id Token: " + challengeResponse.authenticationResult().idToken());

        return "success";
    }
}
