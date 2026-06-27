package fpt.capstone.service.impl;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.KrakenOcrResponse;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.exceprion.enums.ErrorCode;
import fpt.capstone.service.OcrService;
import fpt.capstone.util.OcrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OcrServiceImpl implements OcrService {

    @Value("${kraken.docker.url:http://localhost:8000/ocr}")
    private String krakenUrl;
    private final OcrUtil ocrUtil;
    private SystemLogServiceImpl systemLogService;

    OcrServiceImpl(OcrUtil ocrUtil, SystemLogServiceImpl systemLogService) {
        this.ocrUtil = ocrUtil;
        this.systemLogService = systemLogService;
    }

    @Override
    public ResponseEntity<APIResponse> transformToText(MultipartFile multipartFile) {
        SystemLog systemLog = new SystemLog();
        KrakenOcrResponse result = callKrakenDocker(multipartFile);

        APIResponse apiResponse = APIResponse.builder()
                .data(result)
                .build();

        systemLog.builder()

                .build();
        apiResponse.setCode(ErrorCode.SUCCESS.getCode());
        apiResponse.setMessage(ErrorCode.SUCCESS.getMessage());
        apiResponse.setData(ocrUtil.extractText(multipartFile));
        return ResponseEntity.ok(apiResponse);
    }

    private KrakenOcrResponse callKrakenDocker(MultipartFile file) {
        try{
            RestTemplate restTemplate = new RestTemplate();

            ByteArrayResource fileAsResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileAsResource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Map thẳng vào class KrakenOcrResponse bóc tách dữ liệu
            ResponseEntity<KrakenOcrResponse> response = restTemplate.postForEntity(krakenUrl, requestEntity, KrakenOcrResponse.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

//    @Override
//    public APIResponse<CitizenCardResponse> transformToCitizenCard(MultipartFile multipartFile) {
//        CitizenCardResponse response = new CitizenCardResponse();
//        APIResponse<CitizenCardResponse> apiResponse = new APIResponse<>();
//        String text = ocrUtil.extractText(multipartFile);
//
//        response.setCitizenId(ocrUtil.extractCitizenId(text));
//        response.setDateOfBirth(ocrUtil.extractBirthDate(text));
//        response.setGender(ocrUtil.extractGender(text));
//        response.setFullName(ocrUtil.extractFullName(text));
//        response.setAddress(ocrUtil.extractAddress(text));
//
//        apiResponse.setCode(ErrorCode.SUCCESS.getCode());
//        apiResponse.setMessage(ErrorCode.SUCCESS.getMessage());
//        apiResponse.setData(response);
//
//        return apiResponse;
//    }


}
