package fpt.capstone.exceprion;

import fpt.capstone.dto.response.APIResponse;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

@NoArgsConstructor
public class ArgumentNotValidException extends RuntimeException {

  private List<APIResponse> responses;
    public ArgumentNotValidException(List<APIResponse> responses) {
        super();
        this.responses = responses;
    }

    public List<APIResponse> getResponses() {
        return responses;
    }
}
