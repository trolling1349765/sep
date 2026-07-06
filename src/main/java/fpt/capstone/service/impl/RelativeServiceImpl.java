package fpt.capstone.service.impl;

import fpt.capstone.dto.request.RelativeRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.RelativeResponse;
import fpt.capstone.entity.Relative;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.Table;
import fpt.capstone.repository.BenificiaryRepository;
import fpt.capstone.repository.RelativeRepository;
import fpt.capstone.service.RelativeService;
import fpt.capstone.service.SystemLogService;
import fpt.capstone.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RelativeServiceImpl implements RelativeService {
    private final RelativeRepository relativeRepository;
    private final BenificiaryRepository benificiaryRepository;
    private final SecurityUtil securityUtil;
    private final SystemLogService systemLogService;

    @Override
    public APIResponse<RelativeResponse> createRelative(RelativeRequest relativeRequest) {
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
                .build();
        relative = relativeRepository.save(relative);

        SystemLog systemLog = SystemLog.builder()
                .userId(userId)
                .entityType(Table.RELATIVE.getTableName())
                .entityId(relative.getId() + "")
                .action(Action.RELATIVE_CREATE.getAction())
                .newValue(relative)
                .createdAt(LocalDateTime.now())
                .build();
        systemLogService.write(systemLog);

        APIResponse<RelativeResponse> response = APIResponse.success(new RelativeResponse(relative));
        return response;
    }
}
