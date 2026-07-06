package fpt.capstone.service.impl;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.CccdDetailResponse;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.service.OcrService;
import fpt.capstone.enums.Action;
import fpt.capstone.util.OcrUtil;
import fpt.capstone.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OcrServiceImpl implements OcrService {

    @Value("${kraken.docker.url:http://localhost:8000/ocr}")
    private String krakenUrl;
    private final OcrUtil ocrUtil;
    private final SystemLogServiceImpl systemLogService;
    private final SecurityUtil  securityUtil;

    @Override
    public APIResponse<CccdDetailResponse> extractCCCD(MultipartFile multipartFile) {
        Map<String, String> results = mapCccdGanchip(callKrakenDocker(multipartFile));

        CccdDetailResponse cccdDetailResponse = CccdDetailResponse.builder()
                .idNumber(results.get("idNumber"))
                .fullName(results.get("fullName"))
                .dateOfBirth(results.get("dateOfBirth"))
                .gender(results.get("gender"))
                .nationality(results.get("nationality"))
                .placeOfOrigin(results.get("placeOfOrigin"))
                .placeOfResidence(results.get("placeOfResidence"))
                .issueDate(results.get("issueDate"))
                .issuePlace(results.get("issuePlace"))
                .issueSigner(results.get("issueSigner"))
                .build();
        APIResponse<CccdDetailResponse> response = APIResponse.success(cccdDetailResponse);

        String currentUser = securityUtil.getCurrentUserId();
        SystemLog systemLog = SystemLog.builder()
                .action(Action.OCR_SCAN.getAction())
                .createdAt(LocalDateTime.now())
                .userId(currentUser)
                .build();
        systemLogService.write(systemLog);

        return response;
    }

    @Override
    public ResponseEntity<APIResponse> transformToText(MultipartFile multipartFile) {

        String result = callKrakenDocker(multipartFile);

        APIResponse apiResponse = APIResponse.success(result);

        String currentUser = securityUtil.getCurrentUserId();
        SystemLog systemLog = SystemLog.builder()
                .action(Action.OCR_SCAN.getAction())
                .createdAt(LocalDateTime.now())
                .userId(currentUser)
                .build();
        systemLogService.write(systemLog);

        return ResponseEntity.ok(apiResponse);
    }

    private String callKrakenDocker(MultipartFile file) {
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

            ResponseEntity<String> response = restTemplate.postForEntity(krakenUrl, requestEntity, String.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, String> mapCccdGanchip(String rawOcrText) {
        Map<String, String> resultMap = new LinkedHashMap<>();

        // Bộ Pattern mới phù hợp với nhãn Song ngữ trên CCCD gắn chíp
        Map<String, String> patterns = new LinkedHashMap<>();
        patterns.put("idNumber", "Số\\s*/\\s*No\\.:?\\s*(\\d{12})");
        patterns.put("fullName", "Họ\\s*và\\s*tên\\s*/\\s*Full\\s*name:?\\s*\\n*(.*?)(?=\\n*\\s*(Ngày\\s*sinh|Giới|Quốc|$))");
        patterns.put("dateOfBirth", "Ngày\\s*sinh\\s*/\\s*Date\\s*of\\s*birth:?\\s*(\\d{2}/\\d{2}/\\d{4})");
        patterns.put("gender", "Giới\\s*tính\\s*/\\s*Sex:?\\s*(Nam|Nữ)");
        patterns.put("nationality", "Quốc\\s*tịch\\s*/\\s*Nationality:?\\s*(.*?)(?=\\n|\\s*Quê|$)");
        patterns.put("placeOfOrigin", "Quê\\s*quán\\s*/\\s*Place\\s*of\\s*origin:?\\s*\\n*(.*?)(?=\\n*\\s*(Nơi\\s*thường|Có\\s*giá|$))");
        patterns.put("placeOfResidence", "Nơi\\s*thường\\s*trú\\s*/\\s*Place\\s*of\\s*residence:?\\s*\\n*(.*?)(?=\\n*\\s*(Có\\s*giá|\\d{2}/\\d{2}/|$))");
        patterns.put("issueDate", "Ngày,\\s*tháng,\\s*năm\\s*/\\s*Date,\\s*month,\\s*year\\s*(\\d{2}/\\d{2}/\\d{4})");
        patterns.put("issuePlace", "year\\s*\\d{2}/\\d{2}/\\d{4}\\s*\\n*(.*?)(?=\\n+[^A-ZĂÂĐÊÔƠƯ]*[a-z]|\\n+IDVNM|$)");
        patterns.put("issueSigner", "(?:CỤC TRƯỞNG|CẢNH SÁT|XÃ HỘI)\\s*\\n*(?:.*?\\n){1,3}?(([A-ZĐÁÀẢÃẠÂẤẦẨẪẬĂẮẰẲẴẶÉÈÉẺẼẸÊẾỀỂỄỆÍÌỈĨỊÓÒỎÕỌÔỐỒỔỖỘƠỚỜỞỠỢÚÙỦŨỤƯỨỪỬỮỰ][a-zàáảãạâầấnẩẫậăắằẳẵặèéẻẽẹêềểễệiíìỉĩịòóỏõọôốồổỗộơớờởỡợùúủũụưứừửữự]+\\s*)+)");

        for (Map.Entry<String, String> entry : patterns.entrySet()) {
            Pattern pattern = Pattern.compile(entry.getValue(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher matcher = pattern.matcher(rawOcrText);

            if (matcher.find()) {
                String value = matcher.group(1).trim();
                // Làm sạch các ký tự lạ hoặc dính dấu gạch do OCR nhận diện sai đường kẻ thẻ
                value = value.replaceAll("[_\\-\\|]+", "").trim();
                resultMap.put(entry.getKey(), value.isEmpty() ? "Trống" : value);
            } else {
                resultMap.put(entry.getKey(), "Không tìm thấy");
            }
        }

        return resultMap;
    }
}
