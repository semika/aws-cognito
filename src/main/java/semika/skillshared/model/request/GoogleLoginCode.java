package semika.skillshared.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class GoogleLoginCode implements Serializable {
    private String code;
}
