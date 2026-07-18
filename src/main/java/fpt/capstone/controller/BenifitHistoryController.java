package fpt.capstone.controller;

import fpt.capstone.dto.request.BenifitHistoryRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.BenifitHistoryResponse;
import fpt.capstone.service.BenifitHistoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/benifit-history")
public class BenifitHistoryController {
    BenifitHistoryService benifitHistoryService;

    @GetMapping
    public APIResponse<Page<BenifitHistoryResponse>> getBenifitHistory(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "0") int page
    ) {
        return benifitHistoryService.getBenificiaries(size, page);
    }

    @GetMapping("/beneficiary/{id}")
    public APIResponse getBenefitHistoryByBeneficiaryId(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "0") int page
    ) {
        Page<BenifitHistoryResponse> benifitHistoryResponsePage = benifitHistoryService.getBenefiByBeneficiaryId(id, size, page);
        APIResponse apiResponse = APIResponse.success(benifitHistoryResponsePage);
        return apiResponse;
    }

    @GetMapping("/{id}")
    public APIResponse getBenifitHistoryById(
            @PathVariable Integer id
    ) {
        return APIResponse.success(benifitHistoryService.getBenificiaryHistory(id));
    }

    @PostMapping()
    public APIResponse updateBenifitHistory(
            @RequestBody BenifitHistoryRequest benifitHistoryRequest
    ) {
        return APIResponse.success(benifitHistoryService.create(benifitHistoryRequest));
    }
}
