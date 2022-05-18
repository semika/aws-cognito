package semika.skillshared.service;

import org.springframework.stereotype.Service;
import semika.skillshared.model.request.SignupRequest;
import semika.skillshared.model.response.SignupResponse;

@Service
public class UserSignupService implements UserSignup {

    @Override
    public SignupResponse signup(SignupRequest signupRequest) {

        String USERNAME = "semikas"; // Input an unique username for the UserPool
        String PHONE_NUMBER = "0713258253"; // Input the user phone number for the user Attribute
        String USERPOOL_ID = "ap-southeast-1_YqOJF52e2"; // Input the UserPool Id, e.g. us-east-1_xxxxxxxx
        String USER_TEMP_PASSWORD = "abc123"; // Input the temporary password for the user
        String USER_EMAIL = "semika.siriwardana@gmail.com"; // Input the email for the user attribute

        return SignupResponse.builder().message("succes").build();
    }
}
