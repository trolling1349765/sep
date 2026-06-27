package fpt.capstone.service;

import fpt.capstone.dto.response.PolicyCategoryResponse;
import fpt.capstone.dto.response.PolicyDetailResponse;
import fpt.capstone.dto.response.PolicyListResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CitizenPortalService {
    Page<PolicyListResponse> searchPolicies(String keyword, String category, int page, int size);

    PolicyDetailResponse getPolicyDetail(int policyId);

    List<PolicyCategoryResponse> getCategories();
}