package fpt.capstone.dto.response;

import fpt.capstone.enums.SupportCategory;
import fpt.capstone.enums.SupportRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportRequestDetailResponse {
    private String id;
    private String referenceNumber;
    private String citizenId;
    private String citizenName;
    private String citizenEmail;
    private String citizenPhone;
    private SupportCategory category;
    private String subject;
    private String description;
    private SupportRequestStatus status;
    private String assignedOfficerName;
    private LocalDate submissionDate;
    private Instant resolvedAt;
    private List<AttachmentResponse> attachments;
    private List<SupportReplyResponse> replies;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentResponse {
        private String id;
        private String fileName;
        private String fileType;
        private Long fileSize;
        private String fileUrl;
    }
}