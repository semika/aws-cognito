package semika.skillshared.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class MosaicResendConfirmationCodeRequest implements Serializable {
    private String email;
    private String userName;
}
