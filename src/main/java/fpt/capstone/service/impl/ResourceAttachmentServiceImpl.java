package fpt.capstone.service.impl;

import fpt.capstone.dto.response.AttachmentResponse;
import fpt.capstone.entity.ResourceAttachment;
import fpt.capstone.enums.AttachmentKind;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.Table;
import fpt.capstone.repository.ResourceAttachmentRepository;
import fpt.capstone.service.FileStorageService;
import fpt.capstone.service.ResourceAttachmentService;
import fpt.capstone.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceAttachmentServiceImpl implements ResourceAttachmentService {

    private final ResourceAttachmentRepository resourceAttachmentRepository;
    private final FileStorageService fileStorageService;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional
    public List<AttachmentResponse> saveAll(Table ownerType, String ownerId, AttachmentKind kind,
                                            List<MultipartFile> files) {
        List<AttachmentResponse> saved = new ArrayList<>();
        if (files == null || files.isEmpty()) {
            return saved;
        }
        String actor = securityUtil.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        for (MultipartFile file : files) {
            if (!fileStorageService.isValidFile(file)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.ARGUMENT_INVALID.name());
            }
            String storedName = fileStorageService.storeFile(file);
            ResourceAttachment attachment = ResourceAttachment.builder()
                    .ownerType(ownerType)
                    .ownerId(ownerId)
                    .kind(kind)
                    .fileUrl(storedName)
                    .fileName(file.getOriginalFilename())
                    .uploadedBy(actor)
                    .uploadedAt(now)
                    .createAt(today)
                    .createBy(actor)
                    .build();
            saved.add(AttachmentResponse.from(resourceAttachmentRepository.save(attachment)));
        }
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(Table ownerType, String ownerId) {
        return resourceAttachmentRepository
                .findByOwnerTypeAndOwnerIdOrderByUploadedAtDesc(ownerType, ownerId)
                .stream().map(AttachmentResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponse> listByKind(Table ownerType, String ownerId, AttachmentKind kind) {
        return resourceAttachmentRepository
                .findByOwnerTypeAndOwnerIdAndKindOrderByUploadedAtDesc(ownerType, ownerId, kind)
                .stream().map(AttachmentResponse::from).toList();
    }
}
