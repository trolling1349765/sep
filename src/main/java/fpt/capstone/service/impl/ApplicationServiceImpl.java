package fpt.capstone.service.impl;

import fpt.capstone.dto.request.ApplicationRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.ApplicationResponse;
import fpt.capstone.entity.Application;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.enums.ApplicationStatus;
import fpt.capstone.exceprion.InvalidArgsException;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.repository.ApplicationRepository;
import fpt.capstone.repository.PolicyRepository;
import fpt.capstone.repository.UserRepository;
import fpt.capstone.service.ApplicationService;
import fpt.capstone.service.SystemLogService;
import fpt.capstone.service.FormTypeService;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.Table;
import fpt.capstone.util.AuditJson;
import fpt.capstone.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final SecurityUtil securityUtil;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final SystemLogService systemLogService;
    private final FormTypeService formTypeService;

    @Override
    public Page<ApplicationResponse> getAppications(int size, int page) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Application> applications = applicationRepository.findByDelete(false, pageable);
        Page<ApplicationResponse> applicationResponseList = applications.map(ApplicationResponse::new);

        String currentUserId = securityUtil.getCurrentUserId();
        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.GET_APPLICATIONS.getAction())
                .entityType(Table.APPLICATION.getTableName())
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        return applicationResponseList;
    }

    @Override
    public Page<ApplicationResponse> getAppicationsOFF1(int size, int page) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Application> applications = applicationRepository.findByDeleteAndStatusEquals(false, ApplicationStatus.PENDING, pageable);

        Page<ApplicationResponse> applicationResponses = applications.map(ApplicationResponse::new);

        String currentUserId = securityUtil.getCurrentUserId();
        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.GET_APPLICATIONS.getAction())
                .entityType(Table.APPLICATION.getTableName())
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        return applicationResponses;
    }

    @Override
    public Page<ApplicationResponse> getAppicationBySubmitId(String id, int size, int page) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Application> applications = applicationRepository.findByDeleteFalseAndSubmitByIdEqualsAndStatusEquals(id, ApplicationStatus.DRAFT.get(), pageable);

        Page<ApplicationResponse> applicationResponses = applications.map(ApplicationResponse::new);

        String currentUserId = securityUtil.getCurrentUserId();
        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.GET_APPLICATIONS.getAction())
                .entityType(Table.APPLICATION.getTableName())
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        return applicationResponses;
    }

    @Override
    public Page<ApplicationResponse> getAppicationsOFF2(int size, int page) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Application> applications = applicationRepository.findByDeleteAndStatusEquals(false, ApplicationStatus.CHECKED, pageable);
        Page<ApplicationResponse> applicationResponseList = applications.map(ApplicationResponse::new);


        String currentUserId = securityUtil.getCurrentUserId();
        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.GET_APPLICATIONS.getAction())
                .entityType(Table.APPLICATION.getTableName())
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        return applicationResponseList;
    }

    @Override
    public Page<ApplicationResponse> getAppicationsOFF3(int size, int page) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Application> applications = applicationRepository.findByDeleteAndStatusEquals(false, ApplicationStatus.IN_PROGRESS, pageable);
        Page<ApplicationResponse> applicationResponseList = applications.map(ApplicationResponse::new);

        String currentUserId = securityUtil.getCurrentUserId();
        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.GET_APPLICATIONS.getAction())
                .entityType(Table.APPLICATION.getTableName())
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        return applicationResponseList;
    }

    @Override
    public Page<ApplicationResponse> getAppicationsOFF4(int size, int page) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Application> applications = applicationRepository.findByDeleteAndStatusEquals(false, ApplicationStatus.COMPLETED, pageable);
        Page<ApplicationResponse> applicationResponseList = applications.map(ApplicationResponse::new);

        String currentUserId = securityUtil.getCurrentUserId();
        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.GET_APPLICATIONS.getAction())
                .entityType(Table.APPLICATION.getTableName())
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        return applicationResponseList;
    }

    @Override
    public Page<ApplicationResponse> getAppicationsBySubmiter(String submitBy, int size, int page) {
        requireOwnershipUnlessFullView(submitBy);
        Pageable pageable = PageRequest.of(page, size);
        Page<Application> applications = applicationRepository.findBySubmitBy_IdAndDelete(
                submitBy,
                false,
                pageable);
        Page<ApplicationResponse> applicationResponseList = applications.map(ApplicationResponse::new);

        String currentUserId = securityUtil.getCurrentUserId();
        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.GET_APPLICATIONS.getAction())
                .entityType(Table.APPLICATION.getTableName())
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        return applicationResponseList;
    }

    @Override
    public ApplicationResponse getApplication(int applicationId) {

        Application application = applicationRepository.findById(applicationId).orElse(null);

        if (application == null || application.isDelete()) {
            APIResponse<Object> response = APIResponse.builder()
                    .code(ErrorCode.APPLICATION_NOT_FOUND.getCode())
                    .message(ErrorCode.APPLICATION_NOT_FOUND.getMessage())
                    .data(applicationId)
                    .build();
            throw new InvalidArgsException(response);
        }

        requireOwnershipUnlessFullView(
                application.getSubmitBy() != null ? application.getSubmitBy().getId() : null);

        String currentUserId = securityUtil.getCurrentUserId();
        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.GET_APPLICATIONS.getAction())
                .entityId(application.getId() + "")
                .entityType(Table.APPLICATION.getTableName())
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        return new ApplicationResponse(application);
    }

    /**
     * Ownership guard for the APPLICATION_VIEW_OWN qualifier: callers
     * holding the full APPLICATION_VIEW right may read any record; callers with
     * only the _OWN variant are limited to applications they submitted.
     * Throwing Spring Security's AccessDeniedException routes the request through
     * the AccessDeniedHandler, so the attempt is also audited as ILLEGAL_REQUEST.
     */
    private void requireOwnershipUnlessFullView(String ownerId) {
        if (securityUtil.hasAuthority("APPLICATION_VIEW")) {
            return;
        }
        String currentUserId = securityUtil.getCurrentUserId();
        if (ownerId == null || currentUserId == null || !ownerId.equals(currentUserId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    ErrorCode.ACCESS_DENIED.getMessage());
        }
    }

    @Override
    public Application getApplicationById(int id) {
        Application application = applicationRepository.findById(id).orElse(null);
        if (application == null || application.isDelete())
            return null;
        return application;
    }

    @Override
    public ApplicationResponse createApplication(ApplicationRequest request, String status) {

        String currentUserId = securityUtil.getCurrentUserId();

        Application application = Application.builder()
                .approvedBy(userRepository.getUserById(request.getApprovedBy()))
                .approveDate(request.getApprovedDate())
                .status(ApplicationStatus.valueOf(status))
                .submitDate(request.getSubmitDate())
                .formType(formTypeService.getFormType(request.getFormTypeId()))
                .createAt(LocalDate.now())
                .createBy(currentUserId)
                .isDelete(false)
                .build();

        application = applicationRepository.save(application);

        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.CREATE_APPLICATION.getAction())
                .entityId(application.getId() + "")
                .entityType(Table.APPLICATION.getTableName())
                .newValue(AuditJson.toJson(new ApplicationResponse(application)))
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        return new ApplicationResponse(application);
    }

    @Override
    public ApplicationResponse updateApplication(ApplicationRequest request) {

        String currentUserId = securityUtil.getCurrentUserId();

        Application oldApplication = applicationRepository.findById(request.getId()).orElse(null);
        if (oldApplication == null || oldApplication.isDelete())
            throw new InvalidArgsException(APIResponse.error(
                    ErrorCode.APPLICATION_NOT_FOUND.getCode(),
                    ErrorCode.APPLICATION_NOT_FOUND.getMessage()));
        Application newApplication = Application.builder()
                .approvedBy(userRepository.getUserById(request.getApprovedBy()))
                .approveDate(request.getApprovedDate())
                .submitDate(request.getSubmitDate())
                .status(ApplicationStatus.valueOf(request.getStatus()))
                .formType(formTypeService.getFormType(request.getFormTypeId()))
                .updateAt(LocalDate.now())
                .updateBy(currentUserId)
                .isDelete(request.isDeleted())
                .build();

        newApplication = applicationRepository.save(newApplication);

        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.UPDATE_APPLICATION.getAction())
                .entityId(newApplication.getId() + "")
                .entityType(Table.APPLICATION.getTableName())
                .newValue(AuditJson.toJson(new ApplicationResponse(newApplication)))
                .oldValue(AuditJson.toJson(new ApplicationResponse(oldApplication)))
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        return new ApplicationResponse(newApplication);
    }

    @Override
    public ApplicationResponse toPending(int applicationId) {
        String currentUserId = securityUtil.getCurrentUserId();
        Application application = applicationRepository.getReferenceById(applicationId);
        if (application.isDelete())
            throw new InvalidArgsException(APIResponse.error(ErrorCode.APPLICATION_NOT_FOUND.getCode(),
                    ErrorCode.APPLICATION_NOT_FOUND.getMessage()));
        Application oldApplication = application;

        application.setStatus(ApplicationStatus.PENDING);
        application.setUpdateAt(LocalDate.now());
        application.setUpdateBy(currentUserId);

        applicationRepository.save(application);

        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.UPDATE_APPLICATION.getAction())
                .entityId(applicationId + "")
                .entityType(Table.APPLICATION.getTableName())
                .newValue(AuditJson.toJson(new ApplicationResponse(application)))
                .oldValue(AuditJson.toJson(new ApplicationResponse(oldApplication)))
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        ApplicationResponse response = new ApplicationResponse(application);
        return response;
    }

    @Override
    public ApplicationResponse toChecked(int applicationId) {
        String currentUserId = securityUtil.getCurrentUserId();
        Application application = applicationRepository.getReferenceById(applicationId);
        if (application.isDelete())
            throw new InvalidArgsException(APIResponse.error(ErrorCode.APPLICATION_NOT_FOUND.getCode(),
                    ErrorCode.APPLICATION_NOT_FOUND.getMessage()));

        Application oldApplication = application;

        application.setStatus(ApplicationStatus.CHECKED);
        application.setUpdateAt(LocalDate.now());
        application.setUpdateBy(currentUserId);

        applicationRepository.save(application);

        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.UPDATE_APPLICATION.getAction())
                .entityId(applicationId + "")
                .entityType(Table.APPLICATION.getTableName())
                .newValue(AuditJson.toJson(new ApplicationResponse(application)))
                .oldValue(AuditJson.toJson(new ApplicationResponse(oldApplication)))
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        ApplicationResponse response = new ApplicationResponse(application);
        return response;
    }

    @Override
    public ApplicationResponse toInProgress(int applicationId) {
        String currentUserId = securityUtil.getCurrentUserId();
        Application application = applicationRepository.getReferenceById(applicationId);
        if (application.isDelete())
            throw new InvalidArgsException(APIResponse.error(ErrorCode.APPLICATION_NOT_FOUND.getCode(),
                    ErrorCode.APPLICATION_NOT_FOUND.getMessage()));

        Application oldApplication = application;

        application.setStatus(ApplicationStatus.IN_PROGRESS);
        application.setUpdateAt(LocalDate.now());
        application.setUpdateBy(currentUserId);

        applicationRepository.save(application);

        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.UPDATE_APPLICATION.getAction())
                .entityId(applicationId + "")
                .entityType(Table.APPLICATION.getTableName())
                .newValue(AuditJson.toJson(new ApplicationResponse(application)))
                .oldValue(AuditJson.toJson(new ApplicationResponse(oldApplication)))
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        ApplicationResponse response = new ApplicationResponse(application);
        return response;
    }

    @Override
    public ApplicationResponse toCompleted(int applicationId) {
        String currentUserId = securityUtil.getCurrentUserId();
        Application application = applicationRepository.getReferenceById(applicationId);
        if (application.isDelete())
            throw new InvalidArgsException(APIResponse.error(ErrorCode.APPLICATION_NOT_FOUND.getCode(),
                    ErrorCode.APPLICATION_NOT_FOUND.getMessage()));

        Application oldApplication = application;

        application.setStatus(ApplicationStatus.COMPLETED);
        application.setUpdateAt(LocalDate.now());
        application.setUpdateBy(currentUserId);

        applicationRepository.save(application);

        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.UPDATE_APPLICATION.getAction())
                .entityId(applicationId + "")
                .entityType(Table.APPLICATION.getTableName())
                .newValue(AuditJson.toJson(new ApplicationResponse(application)))
                .oldValue(AuditJson.toJson(new ApplicationResponse(oldApplication)))
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        ApplicationResponse response = new ApplicationResponse(application);
        return response;
    }

    @Override
    public ApplicationResponse toInsufficent(int applicationId) {
        String currentUserId = securityUtil.getCurrentUserId();
        Application application = applicationRepository.getReferenceById(applicationId);
        if (application.isDelete())
            throw new InvalidArgsException(APIResponse.error(ErrorCode.APPLICATION_NOT_FOUND.getCode(),
                    ErrorCode.APPLICATION_NOT_FOUND.getMessage()));

        Application oldApplication = application;

        application.setStatus(ApplicationStatus.INSUFFICIENT);
        application.setUpdateAt(LocalDate.now());
        application.setUpdateBy(currentUserId);

        applicationRepository.save(application);

        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.UPDATE_APPLICATION.getAction())
                .entityId(applicationId + "")
                .entityType(Table.APPLICATION.getTableName())
                .newValue(AuditJson.toJson(new ApplicationResponse(application)))
                .oldValue(AuditJson.toJson(new ApplicationResponse(oldApplication)))
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        ApplicationResponse response = new ApplicationResponse(application);
        return response;
    }

    @Override
    public Object deleteApplication(int applicationId) {
        String currentUserId = securityUtil.getCurrentUserId();
        Application application = applicationRepository.getReferenceById(applicationId);
        if (!application.isDelete())
            throw new InvalidArgsException(APIResponse.error(ErrorCode.APPLICATION_NOT_FOUND.getCode(),
                    ErrorCode.APPLICATION_NOT_FOUND.getMessage()));

        Application oldApplication = application;

        application.setDelete(true);
        application.setUpdateAt(LocalDate.now());
        application.setUpdateBy(currentUserId);

        applicationRepository.save(application);

        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.APPLICATION_DELETE.getAction())
                .entityId(applicationId + "")
                .entityType(Table.APPLICATION.getTableName())
                .newValue(AuditJson.toJson(new ApplicationResponse(application)))
                .oldValue(AuditJson.toJson(new ApplicationResponse(oldApplication)))
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        ApplicationResponse response = new ApplicationResponse(application);
        return response;
    }
}
