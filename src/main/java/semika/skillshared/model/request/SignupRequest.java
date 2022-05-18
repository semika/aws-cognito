package semika.skillshared.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class SignupRequest implements Serializable {
    private String firstName;
    private String lastName;
    private String email;
}
