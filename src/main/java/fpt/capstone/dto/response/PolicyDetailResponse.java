package fpt.capstone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyDetailResponse {
    private int id;
    private String documentNo;
    private String title;
    private String documentType;
    private LocalDate issuedDate;
    private LocalDate effectiveDate;
    private LocalDate expiredDate;
    private String issuer;
    private String summary;
    private String fileURL;
    private List<ArticleResponse> articles;
    private List<EligibilityCriteriaResponse> eligibilityCriterias;
    private BenefitRuleResponse benefitRule;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ArticleResponse {
        private int id;
        private int articleNo;
        private String title;
        private String content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EligibilityCriteriaResponse {
        private int id;
        private String applicableSubject;
        private String conditionValue;
        private String benchmark;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BenefitRuleResponse {
        private int id;
        private String formula;
        private String benchmark;
        private Double multiplier;
    }
}