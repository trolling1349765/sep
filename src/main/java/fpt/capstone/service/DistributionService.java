package fpt.capstone.service;

import fpt.capstone.dto.request.DistributionCreateRequest;
import fpt.capstone.dto.response.DistributionResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.enums.DistributionStatus;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/** Distribution (Cap phat) — issues plan lines and records the append-only history. */
public interface DistributionService {

    PageResponse<DistributionResponse> search(int page, int size, String itemId, Integer beneficiaryId,
                                              String planId, LocalDate fromDate, LocalDate toDate,
                                              DistributionStatus status, String sort, String dir);

    DistributionResponse create(DistributionCreateRequest request, List<MultipartFile> files);

    DistributionResponse confirm(String id);
}
