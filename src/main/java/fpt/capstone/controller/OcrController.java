package fpt.capstone.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fpt.capstone.util.OcrUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ocr")
public class OcrController {

    private final OcrUtil ocrUtil;

    public OcrController(OcrUtil ocrUtil) {
        this.ocrUtil = ocrUtil;
    }

    @PostMapping("/extract")
    public String extractText(@RequestParam("file") MultipartFile file) {
        return ocrUtil.extractText(file);
    }
}
