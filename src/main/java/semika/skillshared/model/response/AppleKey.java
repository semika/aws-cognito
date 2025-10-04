package semika.skillshared.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppleKey implements Serializable {
    private String kty;
    private String kid;
    private String use;
    private String alg;
    private String n;
    private String e;
}
