package semika.skillshared.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserDto implements Serializable {

    private String firstName; // Input an unique username for the UserPool
    private String lastName; // Input the user phone number for the user Attribute
    private String userPoolId; // Input the UserPool Id, e.g. us-east-1_xxxxxxxx
    private String password; // Input the temporary password for the user
    private String email; // Input the email for the user attribute
    private String userName;
    private String phoneNumber;
}
