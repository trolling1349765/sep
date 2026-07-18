package fpt.capstone.controller;

import fpt.capstone.dto.request.BenificiaryRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.BenificiaryResponse;
import fpt.capstone.dto.response.RelativeResponse;
import fpt.capstone.dto.response.WounderSoldierResponse;
import fpt.capstone.entity.Benificiary;
import fpt.capstone.service.BenificiaryService;
import fpt.capstone.service.RelativeService;
import fpt.capstone.service.WounderSoldierService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/benificiary")
public class BenificiaryController {
    private final BenificiaryService benificiaryService;
    private final RelativeService relativeService;
    private final WounderSoldierService wounderSoldierService;

    /**
     * list beneficiary danh cho OFF
     * @param size
     * @param page
     * @return
     */
    @GetMapping
    public APIResponse<Page<BenificiaryResponse>> getBenificiary(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "0") int page
    ) {
        return benificiaryService.getBenificiaries(size, page);
    }

    /**
     * chi tiet 1 beneficiary
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public APIResponse getBenificiary(@PathVariable int id) {
        BenificiaryResponse benificiaryResponse = benificiaryService.getBenificiary(id);
        RelativeResponse relativeResponse = relativeService.getRelativeByApplicationId(benificiaryResponse.getApplicationId());
        List<WounderSoldierResponse> wounderSoldierResponse = wounderSoldierService.getWoundedSoldierByBenificiaryId(benificiaryResponse.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("benificiary", benificiaryResponse);
        data.put("relative", relativeResponse);
        data.put("wounderSoldier", wounderSoldierResponse);

        APIResponse apiResponse = APIResponse.success(data);
        return apiResponse;
    }

    /**
     * ham nay hoi thua (no necessary)
     * @param applicationId
     * @return
     */
    @GetMapping("/application/{id}")
    public APIResponse<List<BenificiaryResponse>> getBenificiariesByApplicationId(@PathVariable int applicationId) {
        return APIResponse.success(benificiaryService.getBenificiariesByApplicationId(applicationId));
    }

    @PutMapping
    public APIResponse updateBenificiary(@RequestBody BenificiaryRequest request) {
        return APIResponse.success(benificiaryService.update(request));
    }

    @DeleteMapping("/{id}")
    public APIResponse deleteBenificiary(@PathVariable int id) {
        return APIResponse.success(benificiaryService.delete(id));
    }
}
