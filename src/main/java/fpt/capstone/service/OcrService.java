package fpt.capstone.service;

import org.springframework.web.multipart.MultipartFile;

public interface OcrService {
    String transformToText(MultipartFile multipartFile);
}
