package fpt.capstone.exceprion;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.exceprion.enums.ErrorCode;
import lombok.Data;

import java.util.List;

@Data
public class AppException extends RuntimeException {

    private List<APIResponse> responses;

    public AppException(List<APIResponse> responses) {
        super();
        this.responses = responses;
    }
}
