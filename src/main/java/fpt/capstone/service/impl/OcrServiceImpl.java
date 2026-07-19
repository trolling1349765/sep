package fpt.capstone.service.impl;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.CccdDetailResponse;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.service.OcrService;
import fpt.capstone.enums.Action;
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
        String normalizedText = normalizeOcrText(rawOcrText);

        // 1. Số CCCD/CMND (Bắt được cả lỗi OCR như S0, Sô, s0, No, Io, ... và lấy chính xác 12 số hoặc 9 số)
        resultMap.put("idNumber", matchPattern(normalizedText, "(?im)(?:s[oó0đb]|no|io|số|s0)\\s*[:/.-]?\\s*(\\d{12}|\\d{9})"));

// 2. Họ và tên (Bắt được các biến thể chữ hoa, chữ thường, mất dấu hoặc sai chính tả của OCR)
        resultMap.put("fullName", extractHeaderValue(normalizedText, "(?i)(?:họ\\s*và\\s*tên|ho\\s*va\\s*ten|ho\\s*va\\s*len|full\\s*name|citizen\\s*identity\\s*card)"));

// 3. Ngày sinh (Định dạng dd/mm/yyyy, dd-mm-yyyy hoặc dd.mm.yyyy)
        resultMap.put("dateOfBirth", matchPattern(normalizedText, "(?im)(?:ngày\\s*sinh|ngay\\s*sinh|dob|date\\s*of\\s*birth|da[:e]{1,4}).*?(\\d{2}[/.\\-]\\d{2}[/.\\-]\\d{4})"));

// 4. Giới tính (Bao gồm cả khoảng trắng thừa do OCR quét lỗi)
        resultMap.put("gender", matchPattern(normalizedText, "(?im)(?:giới\\s*tính|gioi\\s*tinh|sex|gender).*?(nam|nữ|nu|female|male)"));

// 5. Quốc tịch
        resultMap.put("nationality", extractHeaderValue(normalizedText, "(?i)(?:quốc\\s*tịch|quoc\\s*tich|nationality)"));

// 6. Quê quán / Nơi đóng khẩu gốc
        resultMap.put("placeOfOrigin", extractHeaderValue(normalizedText, "(?i)(?:quê\\s*quán|que\\s*quan|place\\s*of\\s*orgin|place\\s*of\\s*origin)"));

// 7. Nơi thường trú / Nơi cư trú
        resultMap.put("placeOfResidence", extractHeaderValue(normalizedText, "(?i)(?:nơi\\s*thường\\s*trú|noi\\s*thuong\\s*tru|nơi\\s*cư\\s*trú|place\\s*af\\s*residence|place\\s*of\\s*residence)"));

// 8. Ngày cấp (Đã tách rời khỏi ngày hết hạn để tránh lấy nhầm dữ liệu)
        resultMap.put("issueDate", matchPattern(normalizedText, "(?im)(?:ngày\\s*cấp|ngay\\s*cap|issued\\s*date|date\\s*of\\s*issue).*?(\\d{2}[/.\\-]\\d{2}[/.\\-]\\d{4})"));

// 9. Ngày hết hạn (Bổ sung thêm trường này cho CCCD mẫu mới)
        resultMap.put("expiryDate", matchPattern(normalizedText, "(?im)(?:có\\s*giá\\s*trị\\s*đến|co\\s*gia\\s*tri\\s*den|date\\s*of\\s*expiry|expiry\\s*date).*?(\\d{2}[/.\\-]\\d{2}[/.\\-]\\d{4})"));

// 10. Nơi cấp
        resultMap.put("issuePlace", extractHeaderValue(normalizedText, "(?i)(?:nơi\\s*cấp|noi\\s*cap|place\\s*of\\s*issue|cục\\s*trưởng|cuc\\s*truong)"));
        String issueSigner = extractHeaderValue(normalizedText, "(?:CUC\\s*TRUONG|CANH\\s*SAT|XA\\s*HOI)");
        if (issueSigner == null) {
            issueSigner = matchPattern(normalizedText, "(?m)(CUC\\s*TRUONG|CANH\\s*SAT|XA\\s*HOI)");
        }
        resultMap.put("issueSigner", issueSigner);

        if (resultMap.get("idNumber") == null) {
            String fallbackId = matchPattern(normalizedText, "(?m)(\\d{12})");
            if (fallbackId != null) {
                resultMap.put("idNumber", fallbackId);
            }
        }

        if (resultMap.get("fullName") == null) {
            String fallbackFullName = findLineAfterKeyword(normalizedText, "(?i)Ho\\s*va\\s*ten|Full\\s*name|Citizen\\s*Identity\\s*Card");
            if (fallbackFullName != null) {
                resultMap.put("fullName", fallbackFullName);
            }
        }

        if (resultMap.get("issuePlace") == null) {
            String fallbackIssuePlace = findLineAfterKeyword(normalizedText, "(?i)Noi\\s*c[aá]p|Place\\s*of\\s*issue|Ngay\\s*c[aá]p");
            if (fallbackIssuePlace != null) {
                resultMap.put("issuePlace", fallbackIssuePlace);
            }
        }

        if (resultMap.get("issuePlace") == null && resultMap.get("issueDate") != null) {
            String fallbackIssuePlace = findLineAfterMatch(normalizedText, resultMap.get("issueDate"));
            if (fallbackIssuePlace != null) {
                resultMap.put("issuePlace", fallbackIssuePlace);
            }
        }

        resultMap.replaceAll((key, value) -> value == null ? "Không tìm thấy" : value);
        return resultMap;
    }

    static Map<String, String> parseCccdText(String rawOcrText) {
        return mapCccdGanchip(rawOcrText);
    }

    private static String normalizeOcrText(String rawOcrText) {
        if (rawOcrText == null) {
            return "";
        }
        String normalized = rawOcrText.replaceAll("\\r\\n?", "\n");
        normalized = normalized.replaceAll("(?i)Place\\s*af\\s*residence", "Place of residence");
        normalized = normalized.replaceAll("(?i)Place\\s*of\\s*orgin", "Place of origin");
        normalized = normalized.replaceAll("(?i)Date\\s*0\\s*oxpiry", "Date of expiry");
        normalized = normalized.replaceAll("(?i)Nalionality", "Nationality");
        normalized = normalized.replaceAll("(?i)Ho\\s*v[aáàảãạâầấẩẫậăắằẳẵặ]*\\s*l[eêếềểễệ]*n", "Ho va ten");
        normalized = normalized.replaceAll("(?i)Ho\\s*v[aáàảãạâầấẩẫậăắằẳẵặ]*\\s*t[eêếềểễệ]+n", "Ho va ten");
        normalized = normalized.replaceAll("(?i)Sociklist Pepuelic CFviet NAX", "Socialist Republic of Viet Nam");
        normalized = normalized.replaceAll("[_\\-\\|]+", " ");
        normalized = normalized.replaceAll("[ \t]+", " ");
        normalized = normalized.replaceAll("(?m)^[ \t]+|[ \t]+$", "");
        normalized = normalized.replaceAll("\n{2,}", "\n");
        return normalized.trim();
    }

    private static String matchPattern(String text, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find() && matcher.groupCount() >= 1) {
            return cleanValue(matcher.group(1));
        }
        return null;
    }

    private static String findLineAfterKeyword(String text, String keywordRegex) {
        Pattern headerPattern = Pattern.compile(keywordRegex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher matcher = headerPattern.matcher(text);
        if (matcher.find()) {
            int end = matcher.end();
            String after = text.substring(end);
            String[] lines = after.split("\\r?\\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    return cleanValue(trimmed);
                }
            }
        }
        return null;
    }

    private static String extractHeaderValue(String text, String headerRegex) {
        Pattern headerPattern = Pattern.compile(headerRegex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.UNICODE_CASE);
        Matcher matcher = headerPattern.matcher(text);
        if (matcher.find()) {
            int end = matcher.end();
            String after = text.substring(end);
            String[] lines = after.split("\r?\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                int colonIndex = trimmed.indexOf(":");
                int dashIndex = trimmed.indexOf("-");
                int delimiterIndex = colonIndex >= 0 ? colonIndex : dashIndex;
                if (delimiterIndex >= 0 && delimiterIndex < trimmed.length() - 1) {
                    String maybeValue = trimmed.substring(delimiterIndex + 1).trim();
                    if (!maybeValue.isEmpty()) {
                        return cleanValue(maybeValue);
                    }
                }
                return cleanValue(trimmed);
            }
        }
        return null;
    }

    private static String findLineAfterMatch(String text, String matchText) {
        int idx = text.indexOf(matchText);
        if (idx >= 0) {
            String after = text.substring(idx + matchText.length());
            String[] lines = after.split("\r?\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    return cleanValue(trimmed);
                }
            }
        }
        return null;
    }

    private static String cleanValue(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("[_\\-\\|]+", "").replaceAll("\\s+", " ").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
