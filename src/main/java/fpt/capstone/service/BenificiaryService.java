package fpt.capstone.service;

import fpt.capstone.dto.request.BenificiaryRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.BenificiaryResponse;
import fpt.capstone.entity.Benificiary;
import org.springframework.data.domain.Page;

public interface BenificiaryService {
    APIResponse<BenificiaryResponse> getBenificiary(int id);

    APIResponse<Page<BenificiaryResponse>> getBenificiary(int size, int page);

    APIResponse<BenificiaryResponse> createBenificiary(BenificiaryRequest benificiaryRequest);
}
