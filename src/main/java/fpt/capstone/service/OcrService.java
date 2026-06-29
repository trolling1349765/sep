package fpt.capstone.service;

import fpt.capstone.dto.response.APIResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {
    ResponseEntity<APIResponse> transformToText(MultipartFile multipartFile);
}
