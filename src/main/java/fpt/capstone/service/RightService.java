package fpt.capstone.service;

import fpt.capstone.dto.request.CreateRightRequest;
import fpt.capstone.dto.request.UpdateRightRequest;
import fpt.capstone.dto.response.RightModuleResponse;
import fpt.capstone.dto.response.RightResponse;

import java.util.List;

public interface RightService {

    /** Full catalogue grouped by module, ordered by module then sortOrder. */
    List<RightModuleResponse> getCatalogue();

    /** Rights are append-only: there is deliberately no delete operation. */
    RightResponse createRight(CreateRightRequest request);

    /** Only name/description are editable; code and isSystem are immutable. */
    RightResponse updateRight(int rightId, UpdateRightRequest request);
}
