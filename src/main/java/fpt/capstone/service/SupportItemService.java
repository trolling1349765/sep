package fpt.capstone.service;

import fpt.capstone.dto.request.SupportItemRequest;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.dto.response.SupportItemResponse;

public interface SupportItemService {

    PageResponse<SupportItemResponse> search(int page, int size, String q, String sort, String dir);

    SupportItemResponse quickCreate(SupportItemRequest request);
}
