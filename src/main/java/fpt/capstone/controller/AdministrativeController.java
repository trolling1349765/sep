package fpt.capstone.controller;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.entity.Permission;
import fpt.capstone.entity.Right;
import fpt.capstone.entity.Role;
import fpt.capstone.entity.User;
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

//    @PostMapping("/user")
//    public APIResponse<User> createUser(@RequestBody User user) {
//
//        return APIResponse.success(null);
//    }
//
//    @PostMapping("/role")
//    public APIResponse<Role> createRole(@RequestBody Role role) {
//
//        return APIResponse.success(null);
//    }
//
//    @PostMapping("/role")
//    public APIResponse<Right> createRight(@RequestBody Right right) {
//
//        return APIResponse.success(null);
//    }
//
//    @PostMapping("/role")
//    public APIResponse<Permission> createPermission(@RequestBody Permission permission) {
//
//        return APIResponse.success(null);
//    }
}