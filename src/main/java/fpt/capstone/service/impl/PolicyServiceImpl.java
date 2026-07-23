package fpt.capstone.service.impl;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.PolicyResponse;
import fpt.capstone.entity.Policy;
import fpt.capstone.repository.PolicyRepository;
import fpt.capstone.service.PolicyService;
import fpt.capstone.service.RightService;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PolicyServiceImpl implements PolicyService {

    PolicyRepository policyRepository;

    @Override
    public Page<PolicyResponse> getPolicies(int size, int page) {
        LocalDate now = LocalDate.now();
        Pageable pageable = PageRequest.of(page, size);
        Page<Policy> policyList = policyRepository.findAllByDeleteAndEffectiveDateLessThanAndExpiredDateGreaterThan(false, now, now, pageable);
        return policyList.map(policy -> new PolicyResponse(policy));
    }
}
