package fpt.capstone.controller;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.WounderSoldierResponse;
import fpt.capstone.service.WounderSoldierService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/wounded")
public class WoundedSoldierController {

    WounderSoldierService wounderSoldierService;

    @GetMapping("/benificiary/{id}")
    @PreAuthorize("hasAuthority('BENEFICIARY_VIEW')")
    public APIResponse<List<WounderSoldierResponse>> getWoundedSoldiersByBenificiary(@PathVariable int benificiaryId) {
        List<WounderSoldierResponse> wounderSoldierResponses = wounderSoldierService.getWoundedSoldierByBenificiaryId(benificiaryId);
        return APIResponse.success(wounderSoldierResponses);
    }
}
