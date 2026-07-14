package fpt.capstone.service.impl;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.ApplicationResponse;
import fpt.capstone.dto.response.BenifitHistoryResponse;
import fpt.capstone.entity.Application;
import fpt.capstone.entity.BenefitHistory;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.ApplicationStatus;
import fpt.capstone.enums.Table;
import fpt.capstone.repository.BenifitHistoryRepository;
import fpt.capstone.service.BenifitHistoryService;
import fpt.capstone.service.SystemLogService;
import fpt.capstone.util.SecurityUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class BenifitHistoryServiceImpl implements BenifitHistoryService {

    BenifitHistoryRepository  benifitHistoryRepository;
    SecurityUtil securityUtil;
    SystemLogService systemLogService;

    @Override
    public APIResponse<Page<BenifitHistoryResponse>> getBenificiaries(int size, int page) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BenefitHistory> benefitHistories = benifitHistoryRepository.findAll(pageable);

        Page<BenifitHistoryResponse> benifitHistoryResponses = benefitHistories.map(BenifitHistoryResponse::new);

        APIResponse<Page<BenifitHistoryResponse>> response = APIResponse.success(benifitHistoryResponses);


        String currentUserId = securityUtil.getCurrentUserId();
        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.GET_APPLICATIONS.getAction())
                .entityType(Table.APPLICATION.getTableName())
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        return response;
    }
}
