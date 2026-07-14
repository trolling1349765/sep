package fpt.capstone.service.impl;

import fpt.capstone.dto.request.BenificiaryRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.BenificiaryResponse;
import fpt.capstone.entity.Benificiary;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.Table;
import fpt.capstone.repository.BenificiaryRepository;
import fpt.capstone.service.ApplicationService;
import fpt.capstone.service.BenificiaryService;
import fpt.capstone.service.SystemLogService;
import fpt.capstone.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BenificiaryServiceImpl implements BenificiaryService {

    private final BenificiaryRepository benificiaryRepository;
    private final ApplicationService applicationService;
    private final SecurityUtil securityUtil;
    private final SystemLogService systemLogService;

    @Override
    public APIResponse<BenificiaryResponse> getBenificiary(int id) {
        Benificiary  benificiary = benificiaryRepository.findById(id).orElse(null);
        if (benificiary == null) {

        }
        BenificiaryResponse benificiaryResponse = new BenificiaryResponse(benificiary);
        APIResponse<BenificiaryResponse> response = APIResponse.success(benificiaryResponse);
        return response;
    }

    @Override
    public List<BenificiaryResponse> getBenificiariesByApplicationId(int applicationId) {
        List<BenificiaryResponse>  benificiaryResponses = new ArrayList<>();
        List<Benificiary> benificiaries = benificiaryRepository.findByApplication(applicationId);
        for(Benificiary benificiary : benificiaries) {
            benificiaryResponses.add(new BenificiaryResponse(benificiary));
        }
        return benificiaryResponses;
    }

    @Override
    public APIResponse<Page<BenificiaryResponse>> getBenificiaries(int size, int page) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Benificiary> benificiaryPage = benificiaryRepository.findAll(pageable);
        Page<BenificiaryResponse> benificiaryResponses = benificiaryPage.map(BenificiaryResponse::new);
        APIResponse<Page<BenificiaryResponse>> response = APIResponse.success(benificiaryResponses);
        return response;
    }

    @Override
    public APIResponse<BenificiaryResponse> createBenificiary(BenificiaryRequest benificiaryRequest) {
        String userId = securityUtil.getCurrentUserId();

        Benificiary benificiary = Benificiary.builder()
                .application(applicationService.getApplicationById(benificiaryRequest.getApplicationId()))
                .gender(benificiaryRequest.isGender())
                .assistanceAmount(benificiaryRequest.getAssistanceAmount())
                .fullName(benificiaryRequest.getFullName())
                .codeName(benificiaryRequest.getCodeName())
                .dob(benificiaryRequest.getDob())
                .CCCD(benificiaryRequest.getCCCD())
                .issuedDate(benificiaryRequest.getIssuedDate())
                .issuedPlace(benificiaryRequest.getIssuedPlace())
                .hometown(benificiaryRequest.getHometown())
                .placeOfResidence(benificiaryRequest.getPlaceOfResidence())
                .joinPartyDate(benificiaryRequest.getJoinPartyDate())
                .officialDate(benificiaryRequest.getOfficialDate())
                .rank(benificiaryRequest.getRank())
                .workUnit(benificiaryRequest.getWorkUnit())
                .beginRevolutionaryActivities(benificiaryRequest.getBeginRevolutionaryActivities())
                .endRevolutionaryActivities(benificiaryRequest.getEndRevolutionaryActivities())
                .sacrificeDate(benificiaryRequest.getSacrificeDate())
                .sacrificeAt(benificiaryRequest.getSacrificeAt())
                .rankWhenSacrifice(benificiaryRequest.getRankWhenSacrifice())
                .nationMeritNumber(benificiaryRequest.getNationMeritNumber())
                .decisionNumberOfMerit(benificiaryRequest.getDecisionNumberOfMerit())
                .recognizedDate(benificiaryRequest.getRecognizedDate())
                .createBy(userId)
                .createAt(LocalDate.now())
                .build();

        benificiary = benificiaryRepository.save(benificiary);
        SystemLog systemLog = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .userId(userId)
                .entityId(Table.BENIFICIARY.getTableName())
                .entityId(benificiary.getId() + "")
                .action(Action.BENIFICIARY_CREATE.getAction())
                .newValue(benificiary)
                .build();
        systemLogService.write(systemLog);

        BenificiaryResponse response = new BenificiaryResponse(benificiary);
        APIResponse<BenificiaryResponse> benificiaryResponse = APIResponse.success(response);

        return benificiaryResponse;
    }
}
