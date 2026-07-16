package fpt.capstone.service.impl;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.PolicyResponse;
import fpt.capstone.entity.Policy;
import fpt.capstone.repository.PolicyRepository;
import fpt.capstone.service.PolicyService;
import fpt.capstone.service.RightService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PolicyServiceImpl implements PolicyService {

    PolicyRepository policyRepository;

    @Override
    public APIResponse<Page<PolicyResponse>> getPolicies(int size, int page) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Policy> policyPage = policyRepository.findAll(pageable);
        Page<PolicyResponse> policyResponses = policyPage.map(PolicyResponse::new);
        APIResponse<Page<PolicyResponse>> response = APIResponse.success(policyResponses);
        return response;
    }
}
