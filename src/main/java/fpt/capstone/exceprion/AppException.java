package fpt.capstone.exceprion;

import fpt.capstone.dto.response.APIResponse;
import lombok.Data;

import java.util.List;

@Data
public class AppException extends RuntimeException {

    private List<APIResponse> responses;

    public AppException(List<APIResponse> responses) {
        super();
        this.responses = responses;
    }

    public void toList(APIResponse apiResponse) {
        responses.add(apiResponse);
    }
}
