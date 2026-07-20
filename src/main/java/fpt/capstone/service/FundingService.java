package fpt.capstone.service;

import fpt.capstone.dto.request.FundingCreateRequest;
import fpt.capstone.dto.response.FundingDetailResponse;
import fpt.capstone.dto.response.FundingListResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.enums.FundingStatus;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/** Funding sources (Nguon kinh phi) — the money side. Backed by the {@code Donation} entity. */
public interface FundingService {

    PageResponse<FundingListResponse> search(int page, int size, FundingStatus status, String sponsorId,
                                             LocalDate fromDate, LocalDate toDate, String sort, String dir);

    FundingDetailResponse getById(int id);

    FundingDetailResponse create(FundingCreateRequest request, List<MultipartFile> files);

    FundingDetailResponse update(int id, FundingCreateRequest request, List<MultipartFile> files);

    FundingDetailResponse confirm(int id);
}
