package fpt.capstone.service.impl;

import fpt.capstone.dto.request.RelativeRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.RelativeResponse;
import fpt.capstone.entity.Relative;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.Table;
import fpt.capstone.repository.ApplicationRepository;
import fpt.capstone.repository.BenificiaryRepository;
import fpt.capstone.repository.RelativeRepository;
import fpt.capstone.service.RelativeService;
import fpt.capstone.service.SystemLogService;
import fpt.capstone.util.AuditJson;
import fpt.capstone.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RelativeServiceImpl implements RelativeService {
    private final RelativeRepository relativeRepository;
    private final ApplicationRepository applicationRepository;
    private final SecurityUtil securityUtil;
    private final SystemLogService systemLogService;

    @Override
    public RelativeResponse createRelative(RelativeRequest relativeRequest, int applicationId) {
        String userId =  securityUtil.getCurrentUserId();

        Relative relative = Relative.builder()
                .fullName(relativeRequest.getFullName())
                .dob(relativeRequest.getDob())
                .CCCD(relativeRequest.getCCCD())
                .issuedDate(relativeRequest.getIssuedDate())
                .issuedPlace(relativeRequest.getIssuedPlace())
                .relationshipWithBenificiary(relativeRequest.getRelationshipWithBenificiary())
                .placeOfResidence(relativeRequest.getPlaceOfResidence())
                .phone(relativeRequest.getPhone())
                .email(relativeRequest.getEmail())
                .gender(relativeRequest.isGender())
                .createAt(LocalDate.now())
                .createBy(userId)
                .application(applicationRepository.getReferenceById(applicationId))
                .build();
        relative = relativeRepository.save(relative);

        SystemLog systemLog = SystemLog.builder()
                .userId(userId)
                .entityType(Table.RELATIVE.getTableName())
                .entityId(relative.getId() + "")
                .action(Action.RELATIVE_CREATE.getAction())
                .newValue(AuditJson.toJson(new RelativeResponse(relative)))
                .createdAt(LocalDateTime.now())
                .build();
        systemLogService.write(systemLog);

        return new RelativeResponse(relative);
    }

    @Override
    public RelativeResponse getRelativeByApplicationId(int applicationId) {
        RelativeResponse response = new RelativeResponse(relativeRepository.findByApplication(applicationId));

        String currentUserId = securityUtil.getCurrentUserId();
        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.RELATIVE_GET.getAction())
                .entityType(Table.RELATIVE.getTableName())
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        return response;
    }
}
