package fpt.capstone.service.impl;

import fpt.capstone.dto.request.CreateSupportRequest;
import fpt.capstone.dto.request.ReplySupportRequest;
import fpt.capstone.dto.request.UpdateSupportRequestStatus;
import fpt.capstone.dto.response.SupportReplyResponse;
import fpt.capstone.dto.response.SupportRequestDetailResponse;
import fpt.capstone.dto.response.SupportRequestListResponse;
import fpt.capstone.entity.*;
import fpt.capstone.enums.NotificationType;
import fpt.capstone.enums.SupportCategory;
import fpt.capstone.enums.SupportRequestStatus;
import fpt.capstone.repository.*;
import fpt.capstone.service.FileStorageService;
import fpt.capstone.service.NotificationService;
import fpt.capstone.service.SupportRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportRequestServiceImpl implements SupportRequestService {

    private final SupportRequestRepository supportRequestRepository;
    private final SupportRequestAttachmentRepository attachmentRepository;
    private final SupportReplyRepository replyRepository;
    private final SystemLogRepository systemLogRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public SupportRequestDetailResponse createSupportRequest(User citizen, CreateSupportRequest request) {
        String referenceNumber = generateReferenceNumber();

        SupportRequest supportRequest = SupportRequest.builder()
                .referenceNumber(referenceNumber)
                .citizen(citizen)
                .category(request.getCategory())
                .subject(request.getSubject())
                .description(request.getDescription())
                .status(SupportRequestStatus.PENDING)
                .createAt(LocalDate.now())
                .createBy(citizen.getId())
                .build();

        supportRequest = supportRequestRepository.save(supportRequest);

        // Attach files if provided
        if (request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty()) {
            List<SupportRequestAttachment> attachments = attachmentRepository.findAllById(request.getAttachmentIds());
            for (SupportRequestAttachment attachment : attachments) {
                attachment.setSupportRequest(supportRequest);
            }
            attachmentRepository.saveAll(attachments);
        }

        log.info("Support request created: {} by citizen {}", referenceNumber, citizen.getEmail());
        return toDetailResponse(supportRequest);
    }

    @Override
    public Page<SupportRequestListResponse> getMyRequests(User citizen, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return supportRequestRepository.findByCitizenIdOrderByCreateAtDesc(citizen.getId(), pageable)
                .map(this::toListResponse);
    }

    @Override
    public SupportRequestDetailResponse getRequestDetail(String requestId, User user) {
        SupportRequest supportRequest = supportRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Support request not found."));

        // Data isolation: citizen can only see their own requests
        boolean isCitizenOwner = supportRequest.getCitizen().getId().equals(user.getId());
        boolean isOfficer = user.getRole().getName().equals("RECEPTION_OFFICER")
                || user.getRole().getName().equals("SOCIAL_AFFAIRS_OFFICER");

        if (!isCitizenOwner && !isOfficer) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
        }

        return toDetailResponse(supportRequest);
    }

    @Override
    public Page<SupportRequestListResponse> getAllRequests(int page, int size,
            SupportRequestStatus status, SupportCategory category,
            LocalDate dateFrom, LocalDate dateTo) {
        Pageable pageable = PageRequest.of(page, size);
        return supportRequestRepository.findAllWithFilters(status, category, dateFrom, dateTo, pageable)
                .map(this::toListResponse);
    }

    @Override
    @Transactional
    public SupportReplyResponse replyToRequest(String requestId, User officer,
            ReplySupportRequest request) {
        SupportRequest supportRequest = supportRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Support request not found."));

        SupportReply reply = SupportReply.builder()
                .supportRequest(supportRequest)
                .officer(officer)
                .message(request.getMessage())
                .createdAt(Instant.now())
                .build();

        reply = replyRepository.save(reply);

        // Auto-assign if not already assigned
        if (supportRequest.getAssignedTo() == null) {
            supportRequest.setAssignedTo(officer);
            supportRequest.setUpdateAt(LocalDate.now());
            supportRequest.setUpdateBy(officer.getId());
            if (supportRequest.getStatus() == SupportRequestStatus.PENDING) {
                supportRequest.setStatus(SupportRequestStatus.IN_PROGRESS);
            }
            supportRequestRepository.save(supportRequest);
        }

        // Audit log
        SystemLog systemLog = SystemLog.builder()
                .userId(officer.getId())
                .action("REPLY_TO_SUPPORT_REQUEST")
                .entityType("SupportRequest")
                .entityId(requestId)
                .newValue("Officer replied: " + request.getMessage())
                .createdAt(new Date())
                .build();
        systemLogRepository.save(systemLog);

        // Notify citizen
        notificationService.sendNotification(
                supportRequest.getCitizen(),
                NotificationType.SUPPORT_REQUEST,
                "New reply to your support request",
                "Your support request " + supportRequest.getReferenceNumber()
                        + " has received a reply from the officer.",
                "SUPPORT_REQUEST",
                supportRequest.getId(),
                null,
                null);

        log.info("Officer {} replied to support request {}", officer.getEmail(), requestId);

        return SupportReplyResponse.builder()
                .id(reply.getId())
                .officerId(officer.getId())
                .officerName(officer.getName())
                .message(reply.getMessage())
                .createdAt(reply.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public SupportRequestDetailResponse updateStatus(String requestId, User officer,
            UpdateSupportRequestStatus request) {
        SupportRequest supportRequest = supportRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Support request not found."));

        SupportRequestStatus oldStatus = supportRequest.getStatus();
        supportRequest.setStatus(request.getStatus());
        supportRequest.setUpdateAt(LocalDate.now());
        supportRequest.setUpdateBy(officer.getId());

        if (request.getStatus() == SupportRequestStatus.RESOLVED) {
            supportRequest.setResolvedAt(Instant.now());
        }

        supportRequestRepository.save(supportRequest);

        // Audit log
        SystemLog systemLog = SystemLog.builder()
                .userId(officer.getId())
                .action("UPDATE_SUPPORT_REQUEST_STATUS")
                .entityType("SupportRequest")
                .entityId(requestId)
                .oldValue(oldStatus.name())
                .newValue(request.getStatus().name())
                .createdAt(new Date())
                .build();
        systemLogRepository.save(systemLog);

        // Notify citizen
        String statusMessage = switch (request.getStatus()) {
            case IN_PROGRESS -> "Your support request is now being processed.";
            case RESOLVED -> "Your support request has been resolved.";
            case PENDING -> "Your support request status has been updated.";
        };

        notificationService.sendNotification(
                supportRequest.getCitizen(),
                NotificationType.SUPPORT_REQUEST,
                "Support request status updated",
                "Support request " + supportRequest.getReferenceNumber() + ": " + statusMessage,
                "SUPPORT_REQUEST",
                supportRequest.getId(),
                null,
                null);

        log.info("Support request {} status changed from {} to {} by officer {}",
                requestId, oldStatus, request.getStatus(), officer.getEmail());

        return toDetailResponse(supportRequest);
    }

    @Override
    @Transactional
    public String uploadAttachment(MultipartFile file) {
        String storedFileName = fileStorageService.storeFile(file);

        SupportRequestAttachment attachment = SupportRequestAttachment.builder()
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .fileUrl(storedFileName)
                .uploadedAt(Instant.now())
                .build();

        attachment = attachmentRepository.save(attachment);
        return attachment.getId();
    }

    private SupportRequestListResponse toListResponse(SupportRequest sr) {
        return SupportRequestListResponse.builder()
                .id(sr.getId())
                .referenceNumber(sr.getReferenceNumber())
                .citizenName(sr.getCitizen().getName())
                .category(sr.getCategory())
                .subject(sr.getSubject())
                .status(sr.getStatus())
                .submissionDate(sr.getCreateAt())
                .build();
    }

    private SupportRequestDetailResponse toDetailResponse(SupportRequest sr) {
        List<SupportReply> replies = replyRepository.findBySupportRequestIdOrderByCreatedAtAsc(sr.getId());
        List<SupportRequestAttachment> attachments = attachmentRepository.findBySupportRequestId(sr.getId());

        return SupportRequestDetailResponse.builder()
                .id(sr.getId())
                .referenceNumber(sr.getReferenceNumber())
                .citizenId(sr.getCitizen().getId())
                .citizenName(sr.getCitizen().getName())
                .citizenEmail(sr.getCitizen().getEmail())
                .citizenPhone(sr.getCitizen().getPhone())
                .category(sr.getCategory())
                .subject(sr.getSubject())
                .description(sr.getDescription())
                .status(sr.getStatus())
                .assignedOfficerName(sr.getAssignedTo() != null ? sr.getAssignedTo().getName() : null)
                .submissionDate(sr.getCreateAt())
                .resolvedAt(sr.getResolvedAt())
                .attachments(attachments.stream()
                        .map(a -> SupportRequestDetailResponse.AttachmentResponse.builder()
                                .id(a.getId())
                                .fileName(a.getFileName())
                                .fileType(a.getFileType())
                                .fileSize(a.getFileSize())
                                .fileUrl(a.getFileUrl())
                                .build())
                        .collect(Collectors.toList()))
                .replies(replies.stream()
                        .map(r -> SupportReplyResponse.builder()
                                .id(r.getId())
                                .officerId(r.getOfficer().getId())
                                .officerName(r.getOfficer().getName())
                                .message(r.getMessage())
                                .createdAt(r.getCreatedAt())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private String generateReferenceNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String refNumber = "SR-" + datePart + "-" + randomPart;

        // Ensure uniqueness
        while (supportRequestRepository.existsByReferenceNumber(refNumber)) {
            randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            refNumber = "SR-" + datePart + "-" + randomPart;
        }

        return refNumber;
    }
}