package fpt.capstone.service.impl;

import fpt.capstone.dto.request.WounderSoldierRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.WounderSoldierResponse;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.entity.WoundedSoldiers;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.Table;
import fpt.capstone.repository.BenificiaryRepository;
import fpt.capstone.repository.WounderSoldierRepository;
import fpt.capstone.service.BenificiaryService;
import fpt.capstone.service.SystemLogService;
import fpt.capstone.service.WounderSoldierService;
import fpt.capstone.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WounderSoldierServiceImpl implements WounderSoldierService {
    private final WounderSoldierRepository wounderSoldierRepository;
    private final SystemLogService systemLogService;
    private final SecurityUtil securityUtil;
    private final BenificiaryRepository benificiaryRepository;

    @Override
    public APIResponse<WounderSoldierResponse> createWounderSoldier(WounderSoldierRequest wounderSoldierRequest) {
        String userId = securityUtil.getCurrentUserId();

        WoundedSoldiers woundedSoldiers = WoundedSoldiers.builder()
                .benificiary(benificiaryRepository.findById(wounderSoldierRequest.getBenificiaryId()).orElse(null))
                .times(wounderSoldierRequest.getTimes())
                .enlistmentDate(wounderSoldierRequest.getEnlistmentDate())
                .dischargeDate(wounderSoldierRequest.getDischargeDate())
                .takeDmgAt(wounderSoldierRequest.getTakeDmgAt())
                .takeDmgDate(wounderSoldierRequest.getTakeDmgDate())
                .rankWhenTakeDmg(wounderSoldierRequest.getRankWhenTakeDmg())
                .injuredArea(wounderSoldierRequest.getInjuredArea())
                .wound(wounderSoldierRequest.getWound())
                .injuryHealedDate(wounderSoldierRequest.getInjuryHealedDate())
                .treatmentPlace(wounderSoldierRequest.getTreatmentPlace())
                .createBy(userId)
                .createAt(LocalDate.now())
                .build();
        woundedSoldiers = wounderSoldierRepository.save(woundedSoldiers);

        SystemLog log = SystemLog.builder()
                .userId(userId)
                .newValue(woundedSoldiers)
                .createdAt(LocalDateTime.now())
                .action(Action.WOUNDER_SOLDIER_CREATE.getAction())
                .entityId(woundedSoldiers.getId() + "")
                .entityType(Table.WOUNDER_SOLDIER.getTableName())
                .build();
        systemLogService.write(log);

        APIResponse<WounderSoldierResponse> response = APIResponse.<WounderSoldierResponse>builder()
                .code(200)
                .data(new WounderSoldierResponse(woundedSoldiers))
                .build();
        return response;
    }

    @Override
    public List<WounderSoldierResponse> getWoundedSoldierByBenificiaryId(int benificiaryId) {
        List<WoundedSoldiers> woundedSoldiers = wounderSoldierRepository.findByBenificiary(benificiaryId);
        List<WounderSoldierResponse> woundedSoldierResponses = new ArrayList<>();
        for (WoundedSoldiers woundedSoldier: woundedSoldiers) {
            woundedSoldierResponses.add(new WounderSoldierResponse(woundedSoldier));
        }

        String currentUserId = securityUtil.getCurrentUserId();
        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.WOUNDER_SOLDIER_GET.getAction())
                .entityType(Table.WOUNDER_SOLDIER.getTableName())
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        return woundedSoldierResponses;
    }
}
