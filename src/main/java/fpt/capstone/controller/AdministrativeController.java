package fpt.capstone.controller;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.service.AdministrativeClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class AdministrativeController {

    private final AdministrativeClient administrativeClient;

    @GetMapping("/provinces")
    public ResponseEntity<APIResponse<List<Map<String, Object>>>> getProvinces() {
        List<Map<String, Object>> provinces = administrativeClient.getProvinces();
        return ResponseEntity.ok(APIResponse.success("Provinces retrieved successfully", provinces));
    }

    @GetMapping("/provinces/{provinceCode}/wards")
    public ResponseEntity<APIResponse<List<Map<String, Object>>>> getWards(
            @PathVariable String provinceCode) {
        List<Map<String, Object>> wards = administrativeClient.getWardsByProvince(provinceCode);
        return ResponseEntity.ok(APIResponse.success("Wards retrieved successfully", wards));
    }
}