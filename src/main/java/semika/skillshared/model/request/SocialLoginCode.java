package semika.skillshared.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class SocialLoginCode implements Serializable {
    private String socialLoginProvier;
    private String code;
}
