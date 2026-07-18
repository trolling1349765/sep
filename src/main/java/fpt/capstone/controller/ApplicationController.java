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
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAuthority('APPLICATION_VIEW')")
    public APIResponse<Page<ApplicationResponse>> getApplication(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        APIResponse<Page<ApplicationResponse>> responses = applicationService.getAppications(size, page);
        return responses;
    }

    @GetMapping("/intake/submitted")
    @PreAuthorize("hasAuthority('APPLICATION_INTAKE_VIEW')")
    public APIResponse<Page<ApplicationResponse>> getApplicationOFF1(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        APIResponse<Page<ApplicationResponse>> responses = applicationService.getAppicationsOFF1(size, page);
        return responses;
    }

    @GetMapping("/intake/pending")
    @PreAuthorize("hasAuthority('APPLICATION_INTAKE_VIEW')")
    public APIResponse<Page<ApplicationResponse>> getApplicationOFF2(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        APIResponse<Page<ApplicationResponse>> responses = applicationService.getAppicationsOFF1(size, page);
        return responses;
    }

    @GetMapping("/intake/checked")
    @PreAuthorize("hasAuthority('APPLICATION_INTAKE_VIEW')")
    public APIResponse<Page<ApplicationResponse>> getApplicationOFF3(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        APIResponse<Page<ApplicationResponse>> responses = applicationService.getAppicationsOFF2(size, page);
        return responses;
    }

    @GetMapping("/intake/in-progress")
    @PreAuthorize("hasAuthority('APPLICATION_INTAKE_VIEW')")
    public APIResponse<Page<ApplicationResponse>> getApplicationOFF4(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        APIResponse<Page<ApplicationResponse>> responses = applicationService.getAppicationsOFF3(size, page);
        return responses;
    }

    @GetMapping("/intake/completed")
    @PreAuthorize("hasAuthority('APPLICATION_INTAKE_VIEW')")
    public APIResponse<Page<ApplicationResponse>> getApplicationOFF(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        APIResponse<Page<ApplicationResponse>> responses = applicationService.getAppicationsOFF4(size, page);
        return responses;
    }

    @GetMapping("/submit-by/{submitBy}")
    @PreAuthorize("hasAnyAuthority('APPLICATION_VIEW', 'APPLICATION_VIEW_OWN')")
    public APIResponse<Page<ApplicationResponse>> getApplicationBySubmiter(
            @PathVariable String submitBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        APIResponse<Page<ApplicationResponse>> responses = applicationService.getAppications(submitBy, size, page);
        return responses;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('APPLICATION_VIEW', 'APPLICATION_VIEW_OWN')")
    public APIResponse getApplicationDetail(@PathVariable int id) {
        ApplicationResponse applicationResponse = applicationService.getApplication(id);
        BenificiaryResponse benificiaryResponse = benificiaryService.getBenificiary(id);
        RelativeResponse relativeResponse = relativeService
                .getRelativeByApplicationId(benificiaryResponse.getApplicationId());
        List<WounderSoldierResponse> wounderSoldierResponse = wounderSoldierService
                .getWoundedSoldierByBenificiaryId(benificiaryResponse.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("benificiary", benificiaryResponse);
        data.put("relative", relativeResponse);
        data.put("wounderSoldier", wounderSoldierResponse);
        data.put("application", applicationResponse);

        APIResponse apiResponse = APIResponse.success(data);
        return APIResponse.success(apiResponse);
    }

    @PutMapping("/to-pending/{applicationId}")
    @PreAuthorize("hasAuthority('APPLICATION_INTAKE')")
    public APIResponse pendingStatusUpdate(@PathVariable int applicationId) {
        ApplicationResponse applicationResponse = applicationService.toPending(applicationId);
        return APIResponse.success(applicationResponse);
    }

    // Workflow-step rights below follow the intake -> appraisal -> approval flow;
    // confirm the exact step alignment with the team if statuses are renamed.
    @PutMapping("/to-checked/{applicationId}")
    @PreAuthorize("hasAuthority('APPLICATION_TRANSFER_TO_APPRAISAL')")
    public APIResponse checkedStatusUpdate(@PathVariable int applicationId) {
        ApplicationResponse applicationResponse = applicationService.toChecked(applicationId);
        return APIResponse.success(applicationResponse);
    }

    @PutMapping("/to-progress/{applicationId}")
    @PreAuthorize("hasAuthority('APPLICATION_APPRAISE')")
    public APIResponse inProgressStatusUpdate(@PathVariable int applicationId) {
        ApplicationResponse applicationResponse = applicationService.toInProgress(applicationId);
        return APIResponse.success(applicationResponse);
    }

    @PutMapping("/to-completed/{applicationId}")
    @PreAuthorize("hasAuthority('APPLICATION_FINAL_APPROVE')")
    public APIResponse completedStatusUpdate(@PathVariable int applicationId) {
        ApplicationResponse applicationResponse = applicationService.toCompleted(applicationId);
        return APIResponse.success(applicationResponse);
    }

    @PutMapping("/add-more/{applicationId}")
    @PreAuthorize("hasAuthority('APPLICATION_REQUEST_SUPPLEMENT_AT_INTAKE')")
    public APIResponse insufficentUpdate(@PathVariable int applicationId) {
        ApplicationResponse applicationResponse = applicationService.toInsufficent(applicationId);
        return APIResponse.success(applicationResponse);
    }

    // No matching right code exists in the 102-right catalogue for deletion:
    // authenticated-only; add a right via
    // POST /admin/rights and annotate when the catalogue is extended.
    @DeleteMapping("/{applicationId}")
    public APIResponse deleteApplication(@PathVariable int applicationId) {
        return APIResponse.success(applicationService.deleteApplication(applicationId));
    }

}
