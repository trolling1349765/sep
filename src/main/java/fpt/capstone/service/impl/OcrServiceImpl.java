package fpt.capstone.service.impl;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.CitizenCardResponse;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.exceprion.enums.ErrorCode;
import fpt.capstone.service.OcrService;
import fpt.capstone.util.OcrUtil;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<APIResponse> transformToText(MultipartFile multipartFile) {
        SystemLog systemLog = new SystemLog();
        APIResponse apiResponse = new APIResponse();

        systemLog.builder()

                .build();
        systemLogService.write(systemLog);
        apiResponse.setCode(ErrorCode.SUCCESS.getCode());
        apiResponse.setMessage(ErrorCode.SUCCESS.getMessage());
        apiResponse.setData(ocrUtil.extractText(multipartFile));
        return ResponseEntity.ok(apiResponse);
    }

    @Override
    public APIResponse<CitizenCardResponse> transformToCitizenCard(MultipartFile multipartFile) {
        CitizenCardResponse response = new CitizenCardResponse();
        APIResponse<CitizenCardResponse> apiResponse = new APIResponse<>();
        String text = ocrUtil.extractText(multipartFile);

        response.setCitizenId(ocrUtil.extractCitizenId(text));
        response.setDateOfBirth(ocrUtil.extractBirthDate(text));
        response.setGender(ocrUtil.extractGender(text));
        response.setFullName(ocrUtil.extractFullName(text));
        response.setAddress(ocrUtil.extractAddress(text));

        apiResponse.setCode(ErrorCode.SUCCESS.getCode());
        apiResponse.setMessage(ErrorCode.SUCCESS.getMessage());
        apiResponse.setData(response);

        return apiResponse;

    }


}
