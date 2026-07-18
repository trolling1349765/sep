package fpt.capstone.controller;

import fpt.capstone.dto.request.BenifitHistoryRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.BenifitHistoryResponse;
import fpt.capstone.service.BenifitHistoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/benifit-history")
public class BenifitHistoryController {
    BenifitHistoryService benifitHistoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('BENEFIT_HISTORY_VIEW')")
    public APIResponse<Page<BenifitHistoryResponse>> getBenifitHistory(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "0") int page) {
        return benifitHistoryService.getBenificiaries(size, page);
    }

    @GetMapping("/beneficiary/{id}")
    @PreAuthorize("hasAuthority('BENEFIT_HISTORY_VIEW')")
    public APIResponse getBenefitHistoryByBeneficiaryId(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "0") int page) {
        Page<BenifitHistoryResponse> benifitHistoryResponsePage = benifitHistoryService.getBenefiByBeneficiaryId(id,
                size, page);
        APIResponse apiResponse = APIResponse.success(benifitHistoryResponsePage);
        return apiResponse;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BENEFIT_HISTORY_VIEW')")
    public APIResponse getBenifitHistoryById(
            @PathVariable Integer id) {
        return APIResponse.success(benifitHistoryService.getBenificiaryHistory(id));
    }

    // No create right exists in the 102-right catalogue for benefit history:
    // authenticated-only.
    @PostMapping()
    public APIResponse updateBenifitHistory(
            @RequestBody BenifitHistoryRequest benifitHistoryRequest) {
        return APIResponse.success(benifitHistoryService.create(benifitHistoryRequest));
    }
}
