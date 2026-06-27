package fpt.capstone.controller;

import fpt.capstone.dto.request.ChatbotQueryRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.ChatbotResponse;
import fpt.capstone.dto.response.PolicyCategoryResponse;
import fpt.capstone.dto.response.PolicyDetailResponse;
import fpt.capstone.dto.response.PolicyListResponse;
import fpt.capstone.entity.User;
import fpt.capstone.service.ChatbotService;
import fpt.capstone.service.CitizenPortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/citizen-portal")
@RequiredArgsConstructor
public class CitizenPortalController {

    private final CitizenPortalService citizenPortalService;
    private final ChatbotService chatbotService;

    // --- Policy Directory ---

    @GetMapping("/policies")
    public ResponseEntity<APIResponse<Page<PolicyListResponse>>> searchPolicies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Page<PolicyListResponse> policies = citizenPortalService.searchPolicies(keyword, category, page, size);
        return ResponseEntity.ok(APIResponse.success(policies));
    }

    @GetMapping("/policies/categories")
    public ResponseEntity<APIResponse<List<PolicyCategoryResponse>>> getCategories() {
        List<PolicyCategoryResponse> categories = citizenPortalService.getCategories();
        return ResponseEntity.ok(APIResponse.success(categories));
    }

    @GetMapping("/policies/{id}")
    public ResponseEntity<APIResponse<PolicyDetailResponse>> getPolicyDetail(@PathVariable int id) {
        PolicyDetailResponse policy = citizenPortalService.getPolicyDetail(id);
        return ResponseEntity.ok(APIResponse.success(policy));
    }

    // --- Chatbot ---

    @PostMapping("/chatbot/query")
    public ResponseEntity<APIResponse<ChatbotResponse>> chat(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChatbotQueryRequest request) {
        ChatbotResponse response = chatbotService.processQuery(user, request.getMessage());
        return ResponseEntity.ok(APIResponse.success(response));
    }
}