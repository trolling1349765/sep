package fpt.capstone.service;

import fpt.capstone.dto.request.AdditionalDocumentRequest;
import fpt.capstone.dto.response.AdditionalDocumentResponse;

import java.util.List;

public interface AdditionalDocumentService {
    List<AdditionalDocumentResponse> getDocumentByApplication(Integer applicationId);

    AdditionalDocumentResponse create(AdditionalDocumentRequest additionalDocumentRequest);

    AdditionalDocumentResponse update(AdditionalDocumentRequest additionalDocumentRequest);

    AdditionalDocumentResponse delete(Integer id);
}
