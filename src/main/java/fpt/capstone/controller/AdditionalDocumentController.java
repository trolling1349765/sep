package fpt.capstone.controller;

import fpt.capstone.dto.request.AdditionalDocumentRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.AdditionalDocumentResponse;
import fpt.capstone.service.AdditionalDocumentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/support-document")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdditionalDocumentController {
    AdditionalDocumentService additionalDocumentService;

    @GetMapping("/application/{id}")
    public APIResponse getSupportDocumentByApplicationId(@PathVariable Integer applicationId) {
        List<AdditionalDocumentResponse> additionalDocumentResponses = additionalDocumentService.getDocumentByApplication(applicationId);
        return APIResponse.success(additionalDocumentResponses);
    }

    @PostMapping
    public APIResponse createAdditionalDocument(@RequestBody AdditionalDocumentRequest additionalDocumentRequest) {
        AdditionalDocumentResponse additionalDocumentResponse = additionalDocumentService.create(additionalDocumentRequest);
        return APIResponse.success(additionalDocumentResponse);
    }

    @PutMapping
    public APIResponse updateAdditionalDocument(@RequestBody AdditionalDocumentRequest additionalDocumentRequest) {
        AdditionalDocumentResponse additionalDocumentResponse = additionalDocumentService.update(additionalDocumentRequest);
        return APIResponse.success(additionalDocumentResponse);
    }

    @DeleteMapping("/{id}")
    public APIResponse deleteAdditionalDocument(@PathVariable Integer id) {
        AdditionalDocumentResponse additionalDocumentResponse = additionalDocumentService.delete(id);
        return APIResponse.success(additionalDocumentResponse);
    }

}
