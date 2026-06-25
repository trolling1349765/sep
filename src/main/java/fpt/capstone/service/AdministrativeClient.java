package fpt.capstone.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdministrativeClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // New API: https://34tinhthanh.com - 2-tier (Province -> Ward)
    private static final String BASE_URL = "https://34tinhthanh.com/api";

    /**
     * Fetch all provinces/cities from 34tinhthanh.com
     * GET /api/provinces
     * Response: [{ "province_code": "01", "name": "Thành phố Hà Nội" }, ...]
     */
    public List<Map<String, Object>> getProvinces() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(BASE_URL + "/provinces", String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            List<Map<String, Object>> result = new ArrayList<>();
            if (root.isArray()) {
                for (JsonNode node : root) {
                    result.add(Map.of(
                            "code", node.get("province_code").asText(),
                            "name", node.get("name").asText()));
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch provinces: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Fetch wards by province code from 34tinhthanh.com
     * GET /api/wards?province_code={code}
     * Response: [{ "ward_code": "00123", "ward_name": "Phường Phúc Xá",
     * "province_code": "01" }, ...]
     */
    public List<Map<String, Object>> getWardsByProvince(String provinceCode) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    BASE_URL + "/wards?province_code=" + provinceCode, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            List<Map<String, Object>> result = new ArrayList<>();
            if (root.isArray()) {
                for (JsonNode node : root) {
                    result.add(Map.of(
                            "code", node.get("ward_code").asText(),
                            "name", node.get("ward_name").asText()));
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch wards for province {}: {}", provinceCode, e.getMessage());
            return List.of();
        }
    }
}