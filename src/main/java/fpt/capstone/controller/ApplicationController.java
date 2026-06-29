package fpt.capstone.controller;

import fpt.capstone.dto.request.ApplicationRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.ApplicationResponse;
import fpt.capstone.entity.Application;
import fpt.capstone.service.ApplicationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.logging.Level;

@RestController
@RequestMapping("/application")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplicationController {

    ApplicationService applicationService;

    @GetMapping()
    public APIResponse<List<ApplicationResponse>> getApplication() {
        return applicationService.getAppications();
    }

    @GetMapping("/{id}")
    public APIResponse<ApplicationResponse> getApplication(@PathVariable int id) {
        return applicationService.getApplication(id);
    }

    @PostMapping
    public APIResponse<ApplicationResponse> createApplication(@RequestBody ApplicationRequest request){
        return applicationService.createApplication(request);
    }

    @PutMapping
    public APIResponse<ApplicationResponse> updateApplication(@RequestBody ApplicationRequest request){
        return applicationService.updateApplication(request);
    }
}
