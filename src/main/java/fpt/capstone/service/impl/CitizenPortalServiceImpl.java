package fpt.capstone.service.impl;

import fpt.capstone.dto.response.PolicyCategoryResponse;
import fpt.capstone.dto.response.PolicyDetailResponse;
import fpt.capstone.dto.response.PolicyListResponse;
import fpt.capstone.entity.Article;
import fpt.capstone.entity.BenefitRule;
import fpt.capstone.entity.EligibilityCriteria;
import fpt.capstone.entity.Policy;
import fpt.capstone.repository.PolicyRepository;
import fpt.capstone.service.CitizenPortalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CitizenPortalServiceImpl implements CitizenPortalService {

    private final PolicyRepository policyRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PolicyListResponse> searchPolicies(String keyword, String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String searchCategory = (category != null && !category.trim().isEmpty()) ? category.trim() : null;

        Page<Policy> policies = policyRepository.searchPolicies(searchKeyword, searchCategory, pageable);

        return policies.map(policy -> PolicyListResponse.builder()
                .id(policy.getId())
                .title(policy.getTitle())
                .summary(policy.getSummary())
                .documentType(policy.getDocumentType())
                .documentNo(policy.getDocumentNo())
                .issuedDate(policy.getIssuedDate())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyDetailResponse getPolicyDetail(int policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Policy not found with id: " + policyId));

        List<PolicyDetailResponse.ArticleResponse> articleResponses = new ArrayList<>();
        if (policy.getChapters() != null) {
            articleResponses = policy.getChapters().get(1).getArticles().stream()
                    .map(article -> PolicyDetailResponse.ArticleResponse.builder()
                            .id(article.getId())
                            .articleNo(article.getArticleNo())
                            .title(article.getTitle())
                            .content(article.getContent())
                            .build())
                    .collect(Collectors.toList());
        }

        List<PolicyDetailResponse.EligibilityCriteriaResponse> criteriaResponses = new ArrayList<>();
        if (policy.getEligibilityCriterias() != null) {
            criteriaResponses = policy.getEligibilityCriterias().stream()
                    .map(criteria -> PolicyDetailResponse.EligibilityCriteriaResponse.builder()
                            .id(criteria.getId())
                            .applicableSubject(criteria.getApplicableSubject())
                            .conditionValue(criteria.getConditionValue())
                            .benchmark(criteria.getBenchmark())
                            .build())
                    .collect(Collectors.toList());
        }

        PolicyDetailResponse.BenefitRuleResponse benefitRuleResponse = null;
        if (policy.getBenefitRule() != null) {
            BenefitRule rule = policy.getBenefitRule();
            benefitRuleResponse = PolicyDetailResponse.BenefitRuleResponse.builder()
                    .id(rule.getId())
                    .formula(rule.getFormula())
                    .benchmark(rule.getBenchmark())
                    .multiplier(rule.getMultiplier())
                    .build();
        }

        return PolicyDetailResponse.builder()
                .id(policy.getId())
                .documentNo(policy.getDocumentNo())
                .title(policy.getTitle())
                .documentType(policy.getDocumentType())
                .issuedDate(policy.getIssuedDate())
                .effectiveDate(policy.getEffectiveDate())
                .expiredDate(policy.getExpiredDate())
                .issuer(policy.getIssuer())
                .summary(policy.getSummary())
                .fileURL(policy.getFileURL())
                .articles(articleResponses)
                .eligibilityCriterias(criteriaResponses)
                .benefitRule(benefitRuleResponse)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyCategoryResponse> getCategories() {
        List<Object[]> results = policyRepository.countByDocumentType();
        return results.stream()
                .map(row -> PolicyCategoryResponse.builder()
                        .documentType((String) row[0])
                        .count((Long) row[1])
                        .build())
                .collect(Collectors.toList());
    }
}