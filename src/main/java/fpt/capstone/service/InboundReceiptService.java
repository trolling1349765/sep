package fpt.capstone.service;

import fpt.capstone.dto.request.InboundReceiptRequest;
import fpt.capstone.dto.response.InboundReceiptDetailResponse;
import fpt.capstone.dto.response.InboundReceiptListResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.enums.ReceiptStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface InboundReceiptService {

    PageResponse<InboundReceiptListResponse> search(int page, int size, String q, ReceiptStatus status,
                                                    String itemId, String sort, String dir);

    InboundReceiptDetailResponse getById(String id);

    InboundReceiptDetailResponse create(InboundReceiptRequest request, List<MultipartFile> files);

    InboundReceiptDetailResponse update(String id, InboundReceiptRequest request, List<MultipartFile> files);

    InboundReceiptDetailResponse post(String id);
}
