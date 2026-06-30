package fpt.capstone.service;

import fpt.capstone.dto.request.ApplicationRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.ApplicationResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ApplicationService {
    APIResponse<Page<ApplicationResponse>> getAppications(int size, int page);

    APIResponse<ApplicationResponse> getApplication(int id);

    APIResponse<ApplicationResponse> createApplication(ApplicationRequest request);

    APIResponse<ApplicationResponse> updateApplication(ApplicationRequest request);
}
