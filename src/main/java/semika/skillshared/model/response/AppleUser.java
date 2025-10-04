package semika.skillshared.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppleUser implements Serializable {
    private String appleId;
    private String email;
    private Boolean emailVerified;
}
