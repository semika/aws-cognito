package semika.skillshared.service;

import semika.skillshared.model.request.MosaicSignupRequest;
import semika.skillshared.model.response.SignupResponse;

public interface UserSignup {
    SignupResponse createPool(MosaicSignupRequest mosaicSignupRequest);
}
