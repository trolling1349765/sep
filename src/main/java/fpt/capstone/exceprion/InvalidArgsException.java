package fpt.capstone.exceprion;

import fpt.capstone.dto.response.APIResponse;

public class InvalidArgsException extends IllegalArgumentException {

    private APIResponse response;

    public InvalidArgsException(APIResponse response) {
        super();
        this.response = response;
    }
}
