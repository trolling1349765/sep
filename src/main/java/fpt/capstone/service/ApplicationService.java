package fpt.capstone.service;

import fpt.capstone.dto.request.ApplicationRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.ApplicationResponse;
import fpt.capstone.entity.Application;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ApplicationService {
    Page<ApplicationResponse> getAppications(int size, int page);

    Page<ApplicationResponse> getAppicationsOFF1(int size, int page);

    Page<ApplicationResponse> getAppicationsOFF2(int size, int page);

    Page<ApplicationResponse> getAppicationsOFF3(int size, int page);

    Page<ApplicationResponse> getAppicationsOFF4(int size, int page);

    Page<ApplicationResponse> getAppicationBySubmitId(String id, int size, int page);

    Page<ApplicationResponse> getAppicationsBySubmiter(String submitBy, int size, int page);

    ApplicationResponse getApplication(int id);

    Application getApplicationById(int id);

    ApplicationResponse createApplication(ApplicationRequest request, String status);

    ApplicationResponse updateApplication(ApplicationRequest request);

    ApplicationResponse toPending(int applicationId);

    ApplicationResponse toChecked(int applicationId);

    ApplicationResponse toInProgress(int applicationId);

    ApplicationResponse toCompleted(int applicationId);

    ApplicationResponse toInsufficent(int applicationId);

    Object deleteApplication(int applicationId);
}
