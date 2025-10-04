package semika.skillshared.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppleAuthorizeUserResponse {

    private Name name;
    private String email;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Name {
        private String firstName;
        private String lastName;
    }
}
