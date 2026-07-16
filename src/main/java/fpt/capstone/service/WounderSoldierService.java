package fpt.capstone.service;

import fpt.capstone.dto.request.WounderSoldierRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.WounderSoldierResponse;
import fpt.capstone.entity.WoundedSoldiers;

import java.util.List;

public interface WounderSoldierService {
    APIResponse<WounderSoldierResponse> createWounderSoldier(WounderSoldierRequest wounderSoldierRequest);

    List<WounderSoldierResponse> getWoundedSoldierByBenificiaryId(int benificiaryId);
}
