package semika.skillshared.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import semika.skillshared.config.AwsConfigProperties;
import semika.skillshared.config.GoogleConfigProperties;
import semika.skillshared.model.request.*;
import semika.skillshared.model.response.SignupResponse;
import semika.skillshared.service.UserSignupService;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@EnableConfigurationProperties(GoogleConfigProperties.class)
@Slf4j
public class AuthController {

    @Autowired
    private UserSignupService userSignupService;

    @Autowired
    private GoogleConfigProperties googleConfigProperties;

    @PostMapping("/createPool")
    public ResponseEntity<SignupResponse> createPool(@RequestBody MosaicSignupRequest mosaicSignupRequest) {
        SignupResponse response = userSignupService.createPool(mosaicSignupRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/msg")
    public ResponseEntity<SignupResponse> message() {
        SignupResponse response = SignupResponse.builder().message("succes").build();
        return new ResponseEntity(response, HttpStatus.OK);
    }

    @PostMapping("/create-user")
    public ResponseEntity<String> createUser(@RequestBody UserDto userDto) {
        String message = userSignupService.createNewUser(userDto);
        return new ResponseEntity<String>(message, HttpStatus.OK);
    }
    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signup(@RequestBody MosaicSignupRequest mosaicSignupRequest) {
        SignUpResponse signUpResponse = userSignupService.signup(mosaicSignupRequest);
        return new ResponseEntity<SignUpResponse>(signUpResponse, HttpStatus.OK);
    }

    @PostMapping("/confirmSignup")
    public ResponseEntity<ConfirmSignUpResponse> confirmSignup(@RequestBody MosaicSingupConfirmRequest mosaicSingupConfirmRequest)
            throws NoSuchAlgorithmException, InvalidKeyException {
        ConfirmSignUpResponse response = userSignupService.confirmSignup(mosaicSingupConfirmRequest);
        return new ResponseEntity<ConfirmSignUpResponse>(response, HttpStatus.OK);
    }

    @PostMapping("/resendCode")
    public ResponseEntity<String> resendConfirmationCode(
            @RequestBody MosaicResendConfirmationCodeRequest mosaicResendConfirmationCodeRequest) {
        String response = userSignupService.resendConfirmationCode(mosaicResendConfirmationCodeRequest);
        return new ResponseEntity<String>(response, HttpStatus.OK);
    }

    @PostMapping("/sms")
    public ResponseEntity<String> sendSms(@RequestParam String phoneNumber) {
        userSignupService.sendSms("+447700185127");
        return new ResponseEntity<>("Success", HttpStatus.OK);
    }

    /*
      Exchange auth code given by Google to get access token.

     */
    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping("/exchange-token")
    public ResponseEntity<String> exchangeGoogleCode(@RequestBody GoogleLoginCode googleLoginCode) throws IOException, InterruptedException {
        log.info("Invoking end point " + googleConfigProperties.getTokenEndPoint()
                +" with code " + googleLoginCode.getCode());

        //Make HttpClient request to google "/token" end point
        //Create HttpClient
        HttpClient client = HttpClient.newHttpClient();

        GoogleTokenRequest googleTokenRequest = new GoogleTokenRequest();
        googleTokenRequest.setCode(googleLoginCode.getCode());
        googleTokenRequest.setClientId(googleConfigProperties.getClientId());
        googleTokenRequest.setClientSecret(googleConfigProperties.getClientSecret());
        googleTokenRequest.setRedirectUri(googleConfigProperties.getRedirectUri());
        googleTokenRequest.setGrantType(googleConfigProperties.getGrantType());

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(googleTokenRequest);
        log.info("Making a request to google token end point with data \n\n" + json);

        // Build POST request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(googleConfigProperties.getTokenEndPoint())) // test endpoint
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        // Send request and get response
        HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString());

        // Print response
        System.out.println("Status code: " + response.statusCode());
        System.out.println("Response body: " + response.body());

        return new ResponseEntity<>("SUCCESS", HttpStatus.OK);
    }

    @PostMapping("/login-with-custom-challenge")
    public ResponseEntity<String> loginWithCustomChallenge(@RequestParam String email) {
        String result =  userSignupService.loginWithCustomChallenge(email);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/response-to-auth-challange")
    public ResponseEntity responseToAuthChallage(@RequestBody CognitoChallangeResponse cognitoChallangeResponse) {
        String result = userSignupService.responseToAuthChallage(cognitoChallangeResponse.getEmail(),
                cognitoChallangeResponse.getAnswer(),
                cognitoChallangeResponse.getSession());
        return new ResponseEntity(result, HttpStatus.OK);
    }
}
