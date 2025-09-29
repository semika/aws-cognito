package semika.skillshared.model.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.Serializable;

@Getter
@Setter
@Builder
public class CognitoChallangeResponse implements Serializable {
    private String answer;
    private String session;
    private String email;
}
