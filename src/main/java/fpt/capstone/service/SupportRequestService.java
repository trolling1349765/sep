package fpt.capstone.service;

import fpt.capstone.dto.request.CreateSupportRequest;
import fpt.capstone.dto.request.ReplySupportRequest;
import fpt.capstone.dto.request.UpdateSupportRequestStatus;
import fpt.capstone.dto.response.SupportReplyResponse;
import fpt.capstone.dto.response.SupportRequestDetailResponse;
import fpt.capstone.dto.response.SupportRequestListResponse;
import fpt.capstone.entity.User;
import fpt.capstone.enums.SupportCategory;
import fpt.capstone.enums.SupportRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public interface SupportRequestService {

        SupportRequestDetailResponse createSupportRequest(User citizen, CreateSupportRequest request);

        Page<SupportRequestListResponse> getMyRequests(User citizen, int page, int size);

        SupportRequestDetailResponse getRequestDetail(String requestId, User user);

        Page<SupportRequestListResponse> getAllRequests(int page, int size,
                        SupportRequestStatus status, SupportCategory category,
                        LocalDate dateFrom, LocalDate dateTo);

        SupportReplyResponse replyToRequest(String requestId, User officer, ReplySupportRequest request);

        SupportRequestDetailResponse updateStatus(String requestId, User officer,
                        UpdateSupportRequestStatus request);

        String uploadAttachment(MultipartFile file);
}