package fpt.capstone.controller;

import fpt.capstone.service.impl.OcrServiceImpl;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ocr")
public class OcrController {

    private OcrServiceImpl ocrService;

    public OcrController(OcrServiceImpl ocrService) {
        this.ocrService = ocrService;
    }

    @PostMapping("/extract")
    public String extractText(@RequestParam("file") MultipartFile file) {
        return ocrService.transformToText(file);
    }
}
