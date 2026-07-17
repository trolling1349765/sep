package fpt.capstone.service.impl;

import fpt.capstone.dto.request.ChatbotAskRequest;
import fpt.capstone.dto.response.ChatbotAskResponse;
import fpt.capstone.service.ChatbotRateLimiterService;
import fpt.capstone.service.ChatbotService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Proxies chatbot questions to the Python chatbot-service (hybrid semantic
 * search). If the sidecar is down or slow (3s timeout), returns a
 * FALLBACK-shaped answer instead of an error so the client UX degrades
 * gracefully. Question logging happens in the sidecar (ask_log table).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotServiceImpl implements ChatbotService {

    // Same wording as the Python service's FALLBACK answer (matcher.py)
    static final String FALLBACK_ANSWER = "Xin lỗi, mình chưa có thông tin để trả lời câu hỏi này. "
            + "Bạn thử diễn đạt lại câu hỏi, hoặc liên hệ bộ phận hỗ trợ của cổng "
            + "trợ cấp xã hội (UBND cấp xã nơi cư trú) để được giải đáp trực tiếp.";

    private final RestClient chatbotRestClient;
    private final ChatbotRateLimiterService chatbotRateLimiterService;

    @Override
    public ChatbotAskResponse ask(ChatbotAskRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String ip = getClientIp(httpRequest);
        ChatbotRateLimiterService.RateLimitResult rateLimit = chatbotRateLimiterService.tryConsume(ip);
        if (!rateLimit.allowed()) {
            httpResponse.setHeader("Retry-After", String.valueOf(rateLimit.retryAfterSeconds()));
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many chatbot requests. Retry after " + rateLimit.retryAfterSeconds() + " seconds.");
        }

        try {
            ChatbotAskResponse response = chatbotRestClient.post()
                    .uri("/api/v1/chatbot/ask")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ChatbotAskRequest(request.getQuestion().trim()))
                    .retrieve()
                    .body(ChatbotAskResponse.class);
            return response != null ? response : fallback();
        } catch (RestClientException e) {
            log.warn("Chatbot service unavailable, returning fallback: {}", e.getMessage());
            return fallback();
        }
    }

    private ChatbotAskResponse fallback() {
        return ChatbotAskResponse.builder()
                .resultType("FALLBACK")
                .answer(FALLBACK_ANSWER)
                .score(0.0)
                .suggestions(List.of())
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}
