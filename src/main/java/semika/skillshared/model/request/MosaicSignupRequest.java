package semika.skillshared.model.request;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class MosaicSignupRequest implements Serializable {
    private String firstName;
    private String lastName;
    private String email;
    private String userName;
    private String phoneNumber;
    private String password;
 }
