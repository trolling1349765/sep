package fpt.capstone.service;

import fpt.capstone.dto.request.ApplicationRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.ApplicationResponse;
import fpt.capstone.entity.Application;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ApplicationService {
    APIResponse<Page<ApplicationResponse>> getAppications(int size, int page);

    APIResponse<Page<ApplicationResponse>> getAppicationsOFF1(int size, int page);

    APIResponse<Page<ApplicationResponse>> getAppicationsOFF2(int size, int page);

    APIResponse<Page<ApplicationResponse>> getAppicationsOFF3(int size, int page);

    APIResponse<Page<ApplicationResponse>> getAppicationsOFF4(int size, int page);

    APIResponse<Page<ApplicationResponse>> getAppicationsDRAFT(int size, int page);

    APIResponse<Page<ApplicationResponse>> getAppications(String submitBy, int size, int page);

    ApplicationResponse getApplication(int id);

    Application getApplicationById(int id);

    APIResponse<ApplicationResponse> createApplication(ApplicationRequest request, String status);

    APIResponse<ApplicationResponse> updateApplication(ApplicationRequest request);

    ApplicationResponse toPending(int applicationId);

    ApplicationResponse toChecked(int applicationId);

    ApplicationResponse toInProgress(int applicationId);

    ApplicationResponse toCompleted(int applicationId);

    ApplicationResponse toInsufficent(int applicationId);

    Object deleteApplication(int applicationId);
}
