package fpt.capstone.service;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.CccdDetailResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {
    ResponseEntity<APIResponse> transformToText(MultipartFile multipartFile);
    APIResponse<CccdDetailResponse> extractCCCD(MultipartFile multipartFile);
}
