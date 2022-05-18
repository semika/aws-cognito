package semika.skillshared.model.response;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class SignupResponse implements Serializable {
    private String message;
}
