package semika.skillshared.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import semika.skillshared.model.request.MosaicSignupRequest;
import semika.skillshared.model.response.SignupResponse;
import semika.skillshared.service.UserSignupService;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserSignupService userSignupService;

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
    public ResponseEntity<String> createUser() {
        userSignupService.createNewUser();
        return new ResponseEntity<String>("User creation successful", HttpStatus.OK);
    }
    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signup(@RequestBody MosaicSignupRequest mosaicSignupRequest) {
        SignUpResponse signUpResponse = userSignupService.signup(mosaicSignupRequest);
        return new ResponseEntity<SignUpResponse>(signUpResponse, HttpStatus.OK);
    }

    @PostMapping("/confirmSignup")
    public ResponseEntity<ConfirmSignUpResponse> confirmSignup(@RequestParam  String code)
            throws NoSuchAlgorithmException, InvalidKeyException {
        ConfirmSignUpResponse response = userSignupService.confirmSignup(code);
        return new ResponseEntity<ConfirmSignUpResponse>(response, HttpStatus.OK);
    }

    @PostMapping("/sms")
    public ResponseEntity<String> sendSms(@RequestParam String phoneNumber) {
        userSignupService.sendSms("+447700185127");
        return new ResponseEntity<>("Success", HttpStatus.OK);
    }
}
