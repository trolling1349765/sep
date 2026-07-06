package fpt.capstone.service;

import fpt.capstone.dto.request.WounderSoldierRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.WounderSoldierResponse;

public interface WounderSoldierService {
    APIResponse<WounderSoldierResponse> createWounderSoldier(WounderSoldierRequest wounderSoldierRequest);
}
