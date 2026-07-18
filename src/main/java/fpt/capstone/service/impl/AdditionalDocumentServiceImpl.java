package fpt.capstone.service.impl;

import fpt.capstone.dto.request.AdditionalDocumentRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.AdditionalDocumentResponse;
import fpt.capstone.entity.AdditionalDocument;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.Table;
import fpt.capstone.exceprion.InvalidArgsException;
import fpt.capstone.repository.AdditionalDocumentRepository;
import fpt.capstone.repository.ApplicationRepository;
import fpt.capstone.service.AdditionalDocumentService;
import fpt.capstone.service.SystemLogService;
import fpt.capstone.util.SecurityUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdditionalDocumentServiceImpl implements AdditionalDocumentService {

    AdditionalDocumentRepository additionalDocumentRepository;
    SecurityUtil securityUtil;
    SystemLogService systemLogService;
    ApplicationRepository applicationRepository;

    @Override
    public List<AdditionalDocumentResponse> getDocumentByApplication(Integer applicationId) {
        List<AdditionalDocument> additionalDocuments = additionalDocumentRepository.findByApplication(applicationId);
        List<AdditionalDocumentResponse> additionalDocumentResponses = additionalDocuments.stream().map(AdditionalDocumentResponse::new).collect(Collectors.toList());

        String currentUserId = securityUtil.getCurrentUserId();
        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.ADDITIONAL_DOCUMENT_GET.getAction())
                .entityType(Table.ADDITIONAL_DOCUMENT.getTableName())
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        return additionalDocumentResponses;
    }

    //file upload cloud
    @Override
    public AdditionalDocumentResponse create(AdditionalDocumentRequest additionalDocumentRequest) {
        String currentUserId = securityUtil.getCurrentUserId();

        AdditionalDocument additionalDocument = AdditionalDocument.builder()
                .createAt(LocalDate.now())
                .type(additionalDocumentRequest.getType())
                .filePath(additionalDocumentRequest.getFilePath())
                .description(additionalDocumentRequest.getDescription())
                .application(applicationRepository.getReferenceById(additionalDocumentRequest.getApplicationId()))
                .createBy(currentUserId)
                .isDelete(false)
                .build();
        additionalDocument = additionalDocumentRepository.save(additionalDocument);

        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.ADDITIONAL_DOCUMENT_CREATE.getAction())
                .entityType(Table.ADDITIONAL_DOCUMENT.getTableName())
                .entityId(additionalDocument.getId() + "")
                .newValue(additionalDocument)
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        AdditionalDocumentResponse response = new AdditionalDocumentResponse(additionalDocument);
        return response;
    }

    @Override
    public AdditionalDocumentResponse update(AdditionalDocumentRequest additionalDocumentRequest) {
        String currentUserId = securityUtil.getCurrentUserId();

        AdditionalDocument additionalDocument = additionalDocumentRepository.getReferenceById(additionalDocumentRequest.getId());
        if (additionalDocument == null) {
            throw new InvalidArgsException(
                    APIResponse.error(
                            ErrorCode.ADDITIONAL_DOCUMENT_NOT_FOUND.getCode(),
                            ErrorCode.ADDITIONAL_DOCUMENT_NOT_FOUND.getMessage()
                    )
            );
        }
        AdditionalDocument oldValue = additionalDocument;
        additionalDocument.setApplication(applicationRepository.getReferenceById(additionalDocumentRequest.getApplicationId()));
        additionalDocument.setDescription(additionalDocumentRequest.getDescription());
        additionalDocument.setFilePath(additionalDocumentRequest.getFilePath());
        additionalDocument.setType(additionalDocumentRequest.getType());
        additionalDocument.setUpdateAt(LocalDate.now());
        additionalDocument.setUpdateBy(currentUserId);
        additionalDocument = additionalDocumentRepository.save(additionalDocument);

        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.ADDITIONAL_DOCUMENT_UPDATE.getAction())
                .entityType(Table.ADDITIONAL_DOCUMENT.getTableName())
                .entityId(additionalDocument.getId() + "")
                .newValue(additionalDocument)
                .oldValue(oldValue)
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        AdditionalDocumentResponse response = new AdditionalDocumentResponse(additionalDocument);
        return response;
    }

    @Override
    public AdditionalDocumentResponse delete(Integer id) {
        String currentUserId = securityUtil.getCurrentUserId();

        AdditionalDocument additionalDocument = additionalDocumentRepository.getReferenceById(id);
        if (additionalDocument == null) {
            throw new InvalidArgsException(APIResponse.error(ErrorCode.ADDITIONAL_DOCUMENT_NOT_FOUND.getCode(), ErrorCode.ADDITIONAL_DOCUMENT_NOT_FOUND.getMessage()));
        }
        AdditionalDocument oldValue = additionalDocument;
        additionalDocument.setDelete(true);
        additionalDocument.setUpdateAt(LocalDate.now());
        additionalDocument.setUpdateBy(currentUserId);
        additionalDocument = additionalDocumentRepository.save(additionalDocument);

        SystemLog log = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.ADDITIONAL_DOCUMENT_DELETE.getAction())
                .entityType(Table.ADDITIONAL_DOCUMENT.getTableName())
                .entityId(additionalDocument.getId() + "")
                .newValue(additionalDocument)
                .oldValue(oldValue)
                .userId(currentUserId)
                .build();
        systemLogService.write(log);

        AdditionalDocumentResponse response = new AdditionalDocumentResponse(additionalDocument);
        return response;
    }
}
