package fpt.capstone.exceprion;

import fpt.capstone.dto.response.APIResponse;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

@Data
@NoArgsConstructor
public class ArgumentNotValidException extends RuntimeException {

  private List<APIResponse> responses;
    public ArgumentNotValidException(List<APIResponse> responses) {
        super();
        this.responses = responses;
    }
}
