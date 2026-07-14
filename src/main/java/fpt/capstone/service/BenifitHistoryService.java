package fpt.capstone.service;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.BenifitHistoryResponse;
import org.springframework.data.domain.Page;

public interface BenifitHistoryService {
    APIResponse<Page<BenifitHistoryResponse>> getBenificiaries(int size, int page);
}
