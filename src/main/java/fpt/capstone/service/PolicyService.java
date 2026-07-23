package fpt.capstone.service;

import fpt.capstone.dto.response.PolicyResponse;
import org.springframework.data.domain.Page;

public interface PolicyService {
    public Page<PolicyResponse> getPolicies(int size, int page);
}
