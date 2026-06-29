package fpt.capstone.service;

import fpt.capstone.dto.request.ApplicationRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.ApplicationResponse;

import java.util.List;

public interface ApplicationService {
    APIResponse<List<ApplicationResponse>> getAppications();

    APIResponse<ApplicationResponse> getApplication(int id);

    APIResponse<ApplicationResponse> createApplication(ApplicationRequest request);

    APIResponse<ApplicationResponse> updateApplication(ApplicationRequest request);
}
