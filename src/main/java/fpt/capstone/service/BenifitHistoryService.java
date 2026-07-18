package fpt.capstone.service;

import fpt.capstone.dto.request.BenifitHistoryRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.BenifitHistoryResponse;
import org.springframework.data.domain.Page;

public interface BenifitHistoryService {
    APIResponse<Page<BenifitHistoryResponse>> getBenificiaries(int size, int page);

    Page<BenifitHistoryResponse> getBenefiByBeneficiaryId(Integer id, int size, int page);

    BenifitHistoryResponse getBenificiaryHistory(Integer id);

    BenifitHistoryResponse create(BenifitHistoryRequest benifitHistoryRequest);
}
