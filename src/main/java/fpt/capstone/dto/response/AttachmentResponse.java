package fpt.capstone.dto.response;

import fpt.capstone.entity.ResourceAttachment;
import fpt.capstone.enums.AttachmentKind;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponse {

    private String id;
    private AttachmentKind kind;
    private String fileName;
    private String fileUrl;
    private String uploadedBy;
    private LocalDateTime uploadedAt;

    public static AttachmentResponse from(ResourceAttachment a) {
        return AttachmentResponse.builder()
                .id(a.getId())
                .kind(a.getKind())
                .fileName(a.getFileName())
                .fileUrl(a.getFileUrl())
                .uploadedBy(a.getUploadedBy())
                .uploadedAt(a.getUploadedAt())
                .build();
    }
}
