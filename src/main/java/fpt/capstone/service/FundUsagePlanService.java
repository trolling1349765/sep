package fpt.capstone.service;

import fpt.capstone.dto.request.FundPlanRequest;
import fpt.capstone.dto.request.ReasonRequest;
import fpt.capstone.dto.response.FundPlanDetailResponse;
import fpt.capstone.dto.response.FundPlanListResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.enums.PlanStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Fund-usage plan lifecycle (Ke hoach su dung kinh phi) — the money state machine. */
public interface FundUsagePlanService {

    PageResponse<FundPlanListResponse> search(int page, int size, String q, Integer donationId,
                                              PlanStatus status, Integer beneficiaryId, String sort, String dir);

    FundPlanDetailResponse getById(String id);

    FundPlanDetailResponse create(FundPlanRequest request, List<MultipartFile> files);

    FundPlanDetailResponse update(String id, FundPlanRequest request, List<MultipartFile> files);

    FundPlanDetailResponse approve(String id);

    FundPlanDetailResponse reject(String id, ReasonRequest request);

    FundPlanDetailResponse cancel(String id, ReasonRequest request);

    FundPlanDetailResponse complete(String id, List<MultipartFile> files);

    FundPlanDetailResponse delete(String id, ReasonRequest request);
}
