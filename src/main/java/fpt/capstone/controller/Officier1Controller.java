package fpt.capstone.controller;

import fpt.capstone.dto.request.*;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.service.ApplicationService;
import fpt.capstone.service.BenificiaryService;
import fpt.capstone.service.RelativeService;
import fpt.capstone.service.WounderSoldierService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/application")
public class Officier1Controller {
    private final ApplicationService applicationService;
    private final BenificiaryService benificiaryService;
    private final WounderSoldierService wounderSoldierService;
    private final RelativeService relativeService;

    @PostMapping("/receipt")
    public APIResponse<String> receiptApplication(@RequestBody Officier1DataObjectRequest officier1DataObjectRequest) {
        ApplicationRequest applicationRequest = officier1DataObjectRequest.getApplicationRequest();
        BenificiaryRequest benificiaryRequest = officier1DataObjectRequest.getBenificiaryRequest();
        RelativeRequest relativeRequest = officier1DataObjectRequest.getRelativeRequest();
        WounderSoldierRequest wounderSoldierRequest = officier1DataObjectRequest.getWounderSoldierRequest();

        applicationService.createApplication(applicationRequest);
        benificiaryService.createBenificiary(benificiaryRequest);
        if (!wounderSoldierRequest.isEmpty()) wounderSoldierService.createWounderSoldier(wounderSoldierRequest);
        if (!relativeRequest.isEmpty()) relativeService.createRelative(relativeRequest);

        return APIResponse.success("Tiếp nhận hồ sơ thành công!");
    }
}
