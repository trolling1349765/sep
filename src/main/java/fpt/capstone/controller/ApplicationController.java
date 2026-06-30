package fpt.capstone.controller;

import fpt.capstone.dto.request.ApplicationRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.ApplicationResponse;
import fpt.capstone.entity.Application;
import fpt.capstone.service.ApplicationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.logging.Level;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplicationController {

    ApplicationService applicationService;

    @GetMapping()
    public APIResponse<Page<ApplicationResponse>> getApplication(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        APIResponse<Page<ApplicationResponse>> responses = applicationService.getAppications(size, page);
        return responses;
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
