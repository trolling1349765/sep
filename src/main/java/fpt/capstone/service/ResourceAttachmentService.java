package fpt.capstone.service;

import fpt.capstone.dto.response.AttachmentResponse;
import fpt.capstone.enums.AttachmentKind;
import fpt.capstone.enums.Table;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Shared attachment store for the donor/resource module. Append-only: there is no delete API
 * for files on state-locked records.
 */
public interface ResourceAttachmentService {

    /** Validates + stores each file and persists a {@code resource_attachments} row. */
    List<AttachmentResponse> saveAll(Table ownerType, String ownerId, AttachmentKind kind,
                                     List<MultipartFile> files);

    List<AttachmentResponse> list(Table ownerType, String ownerId);

    List<AttachmentResponse> listByKind(Table ownerType, String ownerId, AttachmentKind kind);
}
