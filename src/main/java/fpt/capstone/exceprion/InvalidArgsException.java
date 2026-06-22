package fpt.capstone.exceprion;

import fpt.capstone.dto.response.APIResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class InvalidArgsException extends IllegalArgumentException {

    private APIResponse response;

    public InvalidArgsException(APIResponse response) {
        super();
        this.response = response;
    }

}
