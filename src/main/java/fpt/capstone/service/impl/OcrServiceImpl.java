package fpt.capstone.service.impl;

import fpt.capstone.entity.SystemLog;
import fpt.capstone.service.OcrService;
import fpt.capstone.util.OcrUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OcrServiceImpl implements OcrService {

    private final OcrUtil ocrUtil;
    private SystemLogServiceImpl systemLogService;

    OcrServiceImpl(OcrUtil ocrUtil, SystemLogServiceImpl systemLogService) {
        this.ocrUtil = ocrUtil;
        this.systemLogService = systemLogService;
    }

    @Override
    public String transformToText(MultipartFile multipartFile) {
        SystemLog systemLog = new SystemLog();

        systemLog.builder()

                .build();
        systemLogService.write(systemLog);

        return ocrUtil.extractText(multipartFile);
    }
}
