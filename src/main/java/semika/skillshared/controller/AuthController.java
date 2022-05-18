package semika.skillshared.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import semika.skillshared.model.request.SignupRequest;
import semika.skillshared.model.response.SignupResponse;
import semika.skillshared.service.UserSignupService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserSignupService userSignupService;

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@RequestBody SignupRequest signupRequest) {
        SignupResponse response = userSignupService.signup(signupRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/msg")
    public ResponseEntity<SignupResponse> message() {
        SignupResponse response = SignupResponse.builder().message("succes").build();
        return new ResponseEntity(response, HttpStatus.OK);
    }
}
