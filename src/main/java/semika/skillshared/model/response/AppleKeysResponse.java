package semika.skillshared.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppleKeysResponse implements Serializable {
    private List<AppleKey> keys;
}
