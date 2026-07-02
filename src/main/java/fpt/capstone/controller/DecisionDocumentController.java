package fpt.capstone.controller;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.DecisionDocumentResponse;
import fpt.capstone.entity.Application;
import fpt.capstone.entity.DecisionDocument;
import fpt.capstone.service.DecisionDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DecisionDocumentController {

    private final DecisionDocumentService decisionDocumentService;

    @PostMapping
    public APIResponse<DecisionDocumentResponse> createDecisionDocument(
            @RequestBody Application application,
            @RequestBody(required = true) String path
                                                                        ) throws Throwable {
        return decisionDocumentService.createDecisionDocument(application, path);
    }
}
