package semika.skillshared.model.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChallengeResponse {
    private String session;
    private String challengeName;
    private String userName;
}
