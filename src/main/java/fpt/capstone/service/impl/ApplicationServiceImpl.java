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
import fpt.capstone.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final SecurityUtil securityUtil;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;
    private final SystemLogService  systemLogService;
    private final FormTypeService formTypeService;

    @Override
    public APIResponse<Page<ApplicationResponse>> getAppications(int size, int page) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Application> applications = applicationRepository.findAll(pageable);
        Page<ApplicationResponse> applicationResponseList= applications.map(ApplicationResponse::new);

        APIResponse<Page<ApplicationResponse>> response = APIResponse.<Page<ApplicationResponse>>builder()
                .data(applicationResponseList)
                .build();

        String currentUserId = securityUtil.getCurrentUserId();
        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.GET_APPLICATIONS.getAction())
                .entityType(Table.APPLICATION.getTableName())
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        response.setData(applicationResponseList);
        response.setCode(ErrorCode.SUCCESS.getCode());
        response.setMessage(ErrorCode.SUCCESS.getMessage());

        return response;
    }

    @Override
    public APIResponse<ApplicationResponse> getApplication(int applicationId) {

        Application application = applicationRepository.findById(applicationId).orElse(null);

        if (application == null) {
            APIResponse<Object> response = APIResponse.builder()
                    .code(ErrorCode.APPLICATION_NOT_FOUND.getCode())
                    .message(ErrorCode.APPLICATION_NOT_FOUND.getMessage())
                    .data(applicationId)
                    .build();
            throw new InvalidArgsException(response);
        }

        String currentUserId = securityUtil.getCurrentUserId();
        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.GET_APPLICATIONS.getAction())
                .entityId(application.getId()+"")
                .entityType(Table.APPLICATION.getTableName())
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        ApplicationResponse applicationResponse = new ApplicationResponse(application);

        APIResponse<ApplicationResponse> response = APIResponse.success(applicationResponse);

        return response;
    }

    @Override
    public APIResponse<ApplicationResponse> createApplication(ApplicationRequest request) {

        String currentUserId = securityUtil.getCurrentUserId();

        Application application = Application.builder()
                .approvedBy(userRepository.getUserById(request.getApprovedBy()))
                .approveDate(request.getApprovedDate())
                .policy(policyRepository.getPolicyById(request.getPolicyId()))
                .submitDate(request.getSubmitDate())
                .status(ApplicationStatus.valueOf(request.getStatus()))
                .formType(formTypeService.getFormType(Integer.parseInt(request.getFormType())))
                .createAt(LocalDate.now())
                .createBy(currentUserId)
                .isDelete(false)
                .build();

        application = applicationRepository.save(application);

        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.CREATE_APPLICATION.getAction())
                .entityId(application.getId()+"")
                .entityType(Table.APPLICATION.getTableName())
                .newValue(application)
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        return APIResponse.<ApplicationResponse>builder()
                .data(new  ApplicationResponse(application))
                .build();
    }

    @Override
    public APIResponse<ApplicationResponse> updateApplication(ApplicationRequest request) {

        String currentUserId = securityUtil.getCurrentUserId();

        Application oldApplication = applicationRepository.findById(request.getId()).orElse(null);
        if (oldApplication == null) throw new InvalidArgsException(APIResponse.error(
                ErrorCode.APPLICATION_NOT_FOUND.getCode(),
                ErrorCode.APPLICATION_NOT_FOUND.getMessage()
        ));
        Application newApplication = Application.builder()
                .approvedBy(userRepository.getUserById(request.getApprovedBy()))
                .approveDate(request.getApprovedDate())
                .policy(policyRepository.getPolicyById(request.getPolicyId()))
                .submitDate(request.getSubmitDate())
                .status(ApplicationStatus.valueOf(request.getStatus()))
                .formType(formTypeService.getFormType(Integer.parseInt(request.getFormType())))
                .updateAt(LocalDate.now())
                .updateBy(currentUserId)
                .isDelete(request.isDelete())
                .build();

        newApplication = applicationRepository.save(newApplication);

        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.UPDATE_APPLICATION.getAction())
                .entityId(newApplication.getId()+"")
                .entityType(Table.APPLICATION.getTableName())
                .newValue(newApplication)
                .oldValue(oldApplication)
                .userId(currentUserId)
                .build();

        systemLogService.write(log);

        return APIResponse.<ApplicationResponse>builder()
                .data(new  ApplicationResponse(newApplication))
                .build();
    }
}
