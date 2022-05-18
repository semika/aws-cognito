package semika.skillshared.service;

import semika.skillshared.model.request.SignupRequest;
import semika.skillshared.model.response.SignupResponse;

public interface UserSignup {
    SignupResponse signup(SignupRequest signupRequest);
}
