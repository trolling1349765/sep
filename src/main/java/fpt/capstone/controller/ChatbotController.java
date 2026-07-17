package fpt.capstone.controller;

import fpt.capstone.dto.request.ChatbotAskRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.ChatbotAskResponse;
import fpt.capstone.service.ChatbotService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/ask")
    public ResponseEntity<APIResponse<ChatbotAskResponse>> ask(
            @Valid @RequestBody ChatbotAskRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        return ResponseEntity.ok(APIResponse.success(chatbotService.ask(request, httpRequest, httpResponse)));
    }
}
