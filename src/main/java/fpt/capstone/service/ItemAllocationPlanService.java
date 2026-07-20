package fpt.capstone.service;

import fpt.capstone.dto.request.ItemPlanRequest;
import fpt.capstone.dto.request.NotDeliveredRequest;
import fpt.capstone.dto.request.ReasonRequest;
import fpt.capstone.dto.response.ItemPlanDetailResponse;
import fpt.capstone.dto.response.ItemPlanListResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.enums.PlanStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Item allocation plan lifecycle (Ke hoach phan bo vat pham) — the stock state machine. */
public interface ItemAllocationPlanService {

    PageResponse<ItemPlanListResponse> search(int page, int size, String q, String itemId,
                                              PlanStatus status, String sort, String dir);

    ItemPlanDetailResponse getById(String id);

    ItemPlanDetailResponse create(ItemPlanRequest request, List<MultipartFile> files);

    ItemPlanDetailResponse update(String id, ItemPlanRequest request, List<MultipartFile> files);

    ItemPlanDetailResponse approve(String id);

    ItemPlanDetailResponse reject(String id, ReasonRequest request);

    ItemPlanDetailResponse cancel(String id, ReasonRequest request);

    ItemPlanDetailResponse delete(String id, ReasonRequest request);

    ItemPlanDetailResponse markNotDelivered(String lineId, NotDeliveredRequest request);
}
