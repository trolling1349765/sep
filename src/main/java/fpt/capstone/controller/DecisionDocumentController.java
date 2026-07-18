package fpt.capstone.controller;

import fpt.capstone.dto.response.DecisionDocumentResponseWrapper;
import fpt.capstone.entity.Application;
import fpt.capstone.service.DecisionDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DecisionDocumentController {

    private final DecisionDocumentService decisionDocumentService;



    @PostMapping("/download-decision")
    @PreAuthorize("hasAuthority('OFFICIAL_DOCUMENT_VIEW')")
    public ResponseEntity<byte[]> downloadDecisionDocument(@RequestBody Application application) {
        try {
            // Gọi service xử lý
            DecisionDocumentResponseWrapper wrapper = decisionDocumentService.createDecisionDocumentWrapper(application);

            // Cấu hình Header để trình duyệt hiểu đây là file download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", wrapper.getFileName());
            // Tránh lỗi hiển thị ký tự đặc biệt ở tên file:
            headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(wrapper.getFileContent());

        } catch (Throwable e) {
            // Xử lý exception theo cấu trúc project của bạn
            return ResponseEntity.internalServerError().build();
        }
    }
}
