package fpt.capstone.controller;

import fpt.capstone.dto.request.ApplicationRequest;
import fpt.capstone.dto.response.*;
import fpt.capstone.entity.Application;
import fpt.capstone.entity.WoundedSoldiers;
import fpt.capstone.service.ApplicationService;
import fpt.capstone.service.BenificiaryService;
import fpt.capstone.service.RelativeService;
import fpt.capstone.service.WounderSoldierService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.function.EntityResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApplicationController {

    ApplicationService applicationService;
    BenificiaryService benificiaryService;
    RelativeService relativeService;
    WounderSoldierService wounderSoldierService;

    @GetMapping()
    public APIResponse<Page<ApplicationResponse>> getApplication(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        APIResponse<Page<ApplicationResponse>> responses = applicationService.getAppications(size, page);
        return responses;
    }

    @GetMapping("/intake/pending")
    public APIResponse<Page<ApplicationResponse>> getApplicationOFF1(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        APIResponse<Page<ApplicationResponse>> responses = applicationService.getAppicationsOFF1(size, page);
        return responses;
    }

    @GetMapping("/intake/checked")
    public APIResponse<Page<ApplicationResponse>> getApplicationOFF2(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        APIResponse<Page<ApplicationResponse>> responses = applicationService.getAppicationsOFF2(size, page);
        return responses;
    }

    @GetMapping("/intake/in-progress")
    public APIResponse<Page<ApplicationResponse>> getApplicationOFF3(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        APIResponse<Page<ApplicationResponse>> responses = applicationService.getAppicationsOFF3(size, page);
        return responses;
    }

    @GetMapping("/intake/completed")
    public APIResponse<Page<ApplicationResponse>> getApplicationOFF4(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        APIResponse<Page<ApplicationResponse>> responses = applicationService.getAppicationsOFF4(size, page);
        return responses;
    }

    @GetMapping("/submit-by/{id}")
    public APIResponse<Page<ApplicationResponse>> getApplicationBySubmiter(
            @PathVariable String submitBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        APIResponse<Page<ApplicationResponse>> responses = applicationService.getAppications(submitBy, size, page);
        return responses;
    }

    @GetMapping("/{id}")
    public APIResponse getApplicationDetail(@PathVariable int id) {
        ApplicationResponse applicationResponse = applicationService.getApplication(id);
        BenificiaryResponse benificiaryResponse = benificiaryService.getBenificiary(id);
        RelativeResponse relativeResponse = relativeService.getRelativeByApplicationId(benificiaryResponse.getApplicationId());
        List<WounderSoldierResponse> wounderSoldierResponse = wounderSoldierService.getWoundedSoldierByBenificiaryId(benificiaryResponse.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("benificiary", benificiaryResponse);
        data.put("relative", relativeResponse);
        data.put("wounderSoldier", wounderSoldierResponse);
        data.put("application", applicationResponse);

        APIResponse apiResponse = APIResponse.success(data);
        return APIResponse.success(apiResponse);
    }
}
