package fpt.capstone.service;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.DecisionDocumentResponse;
import fpt.capstone.dto.response.DecisionDocumentResponseWrapper;
import fpt.capstone.entity.Application;
import fpt.capstone.entity.DecisionDocument;
import org.springframework.data.domain.Page;

public interface DecisionDocumentService {
    DecisionDocumentResponseWrapper createDecisionDocumentWrapper(Application application) throws Throwable;

    APIResponse<DecisionDocumentResponse> getDecisionDocument(int id);

    APIResponse<Page<DecisionDocumentResponse>> getAllDecisionDocuments(int size, int page);
}
