package fpt.capstone.service;

import fpt.capstone.dto.request.BenificiaryRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.BenificiaryResponse;
import fpt.capstone.entity.Benificiary;
import org.springframework.data.domain.Page;

import java.util.List;

public interface BenificiaryService {
    BenificiaryResponse getBenificiary(int id);

    List<BenificiaryResponse> getBenificiariesByApplicationId(int applicationId);

    Page<BenificiaryResponse> getBenificiaries(int size, int page);

    BenificiaryResponse createBenificiary(BenificiaryRequest benificiaryRequest, int applicationId);

    BenificiaryResponse update(BenificiaryRequest request);

    BenificiaryResponse delete(int id);
}
