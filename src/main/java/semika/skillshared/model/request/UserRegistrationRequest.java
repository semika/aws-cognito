package semika.skillshared.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserRegistrationRequest implements Serializable {
    private String userSub;
    private String firstName;
    private String lastName;
    private String email;
}
