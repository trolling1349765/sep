package fpt.capstone.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response of the Python chatbot-service. @JsonAlias only affects
 * deserialization, so the service's snake_case payload maps in while this API
 * still returns camelCase like every other DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotAskResponse {

    /** MATCHED | UNSURE | FALLBACK */
    @JsonAlias("result_type")
    private String resultType;

    private String answer;

    @JsonAlias("matched_question")
    private String matchedQuestion;

    private Double score;

    private List<Suggestion> suggestions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Suggestion {
        private Long id;
        private String question;
        private Double score;
    }
}
