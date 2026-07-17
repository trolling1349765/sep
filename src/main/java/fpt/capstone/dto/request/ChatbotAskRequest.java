package fpt.capstone.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotAskRequest {

    @NotBlank(message = "Question is required")
    @Size(max = 500, message = "Question must be at most 500 characters")
    private String question;
}
