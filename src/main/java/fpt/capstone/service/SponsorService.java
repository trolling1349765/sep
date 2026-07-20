package fpt.capstone.service;

import fpt.capstone.dto.request.SponsorRequest;
import fpt.capstone.dto.request.UpdateSponsorStatusRequest;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.dto.response.SponsorDetailResponse;
import fpt.capstone.dto.response.SponsorListResponse;
import fpt.capstone.enums.SponsorStatus;
import fpt.capstone.enums.SponsorType;

public interface SponsorService {

    PageResponse<SponsorListResponse> search(int page, int size, String q,
                                             SponsorType type, SponsorStatus status,
                                             String sort, String dir);

    SponsorDetailResponse getById(String id);

    SponsorDetailResponse create(SponsorRequest request);

    SponsorDetailResponse update(String id, SponsorRequest request);

    SponsorDetailResponse changeStatus(String id, UpdateSponsorStatusRequest request);
}
