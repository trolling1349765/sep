package fpt.capstone.service.impl;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.BenificiaryResponse;
import fpt.capstone.entity.Benificiary;
import fpt.capstone.repository.BenificiaryRepository;
import fpt.capstone.service.BenificiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BenificiaryServiceImpl implements BenificiaryService {

    private final BenificiaryRepository benificiaryRepository;

    @Override
    public APIResponse<BenificiaryResponse> getBenificiary(int id) {
        Benificiary  benificiary = benificiaryRepository.findById(id).orElse(null);
        if (benificiary == null) {

        }
        BenificiaryResponse benificiaryResponse = new BenificiaryResponse(benificiary);
        APIResponse<BenificiaryResponse> response = APIResponse.success(benificiaryResponse);
        return response;
    }

    @Override
    public APIResponse<Page<BenificiaryResponse>> getBenificiary(int size, int page) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Benificiary> benificiaryPage = benificiaryRepository.findAll(pageable);
        Page<BenificiaryResponse> benificiaryResponses = benificiaryPage.map(BenificiaryResponse::new);
        APIResponse<Page<BenificiaryResponse>> response = APIResponse.success(benificiaryResponses);
        return response;
    }

    @Override
    public APIResponse<BenificiaryResponse> createBenificiary(Benificiary benificiary) {
        BenificiaryResponse response = new BenificiaryResponse(benificiaryRepository.save(benificiary));
        APIResponse<BenificiaryResponse> benificiaryResponse = APIResponse.success(response);
        return benificiaryResponse;
    }
}
