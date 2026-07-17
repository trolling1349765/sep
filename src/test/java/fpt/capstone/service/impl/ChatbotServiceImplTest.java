package fpt.capstone.service.impl;

import fpt.capstone.dto.request.ChatbotAskRequest;
import fpt.capstone.dto.response.ChatbotAskResponse;
import fpt.capstone.service.ChatbotRateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceImplTest {

    private static final String ASK_URL = "http://localhost:8001/api/v1/chatbot/ask";

    @Mock
    private ChatbotRateLimiterService chatbotRateLimiterService;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private HttpServletResponse httpResponse;

    private MockRestServiceServer server;
    private ChatbotServiceImpl chatbotService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8001");
        server = MockRestServiceServer.bindTo(builder).build();
        chatbotService = new ChatbotServiceImpl(builder.build(), chatbotRateLimiterService);

        lenient().when(httpRequest.getRemoteAddr()).thenReturn("10.0.0.1");
        lenient().when(chatbotRateLimiterService.tryConsume(anyString()))
                .thenReturn(new ChatbotRateLimiterService.RateLimitResult(true, 0));
    }

    private ChatbotAskResponse ask(String question) {
        return chatbotService.ask(new ChatbotAskRequest(question), httpRequest, httpResponse);
    }

    private void assertFallback(ChatbotAskResponse response) {
        assertEquals("FALLBACK", response.getResultType());
        assertEquals(ChatbotServiceImpl.FALLBACK_ANSWER, response.getAnswer());
        assertNull(response.getMatchedQuestion());
        assertEquals(0.0, response.getScore());
        assertTrue(response.getSuggestions().isEmpty());
    }

    @Nested
    class AskTests {

        @Test
        void ask_matchedResponse_mapsSnakeCaseFields() {
            server.expect(requestTo(ASK_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(jsonPath("$.question").value("tro cap nguoi cao tuoi"))
                    .andRespond(withSuccess("""
                            {"result_type":"MATCHED","answer":"Tra loi",
                             "matched_question":"Cau hoi goc","score":0.91,
                             "suggestions":[{"id":3,"question":"Cau lien quan","score":0.8}]}
                            """, MediaType.APPLICATION_JSON));

            ChatbotAskResponse response = ask("tro cap nguoi cao tuoi");

            assertEquals("MATCHED", response.getResultType());
            assertEquals("Tra loi", response.getAnswer());
            assertEquals("Cau hoi goc", response.getMatchedQuestion());
            assertEquals(0.91, response.getScore());
            assertEquals(1, response.getSuggestions().size());
            assertEquals(3L, response.getSuggestions().get(0).getId());
            assertEquals("Cau lien quan", response.getSuggestions().get(0).getQuestion());
            server.verify();
        }

        @Test
        void ask_trimsQuestionBeforeProxying() {
            server.expect(requestTo(ASK_URL))
                    .andExpect(jsonPath("$.question").value("xin chao"))
                    .andRespond(withSuccess("""
                            {"result_type":"MATCHED","answer":"A","matched_question":"Q",
                             "score":0.9,"suggestions":[]}
                            """, MediaType.APPLICATION_JSON));

            ask("   xin chao   ");
            server.verify();
        }

        @Test
        void ask_serverError_returnsFallback() {
            server.expect(requestTo(ASK_URL)).andRespond(withServerError());

            assertFallback(ask("cau hoi"));
        }

        @Test
        void ask_timeout_returnsFallback() {
            server.expect(requestTo(ASK_URL))
                    .andRespond(withException(new SocketTimeoutException("read timed out")));

            assertFallback(ask("cau hoi"));
        }

        @Test
        void ask_connectionRefused_returnsFallback() {
            server.expect(requestTo(ASK_URL))
                    .andRespond(withException(new ConnectException("Connection refused")));

            assertFallback(ask("cau hoi"));
        }
    }

    @Nested
    class RateLimitTests {

        @Test
        void ask_rateLimitExceeded_throws429WithRetryAfterHeader() {
            when(chatbotRateLimiterService.tryConsume(anyString()))
                    .thenReturn(new ChatbotRateLimiterService.RateLimitResult(false, 42));

            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> ask("cau hoi"));

            assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
            verify(httpResponse).setHeader("Retry-After", "42");
        }

        @Test
        void ask_xForwardedFor_firstIpUsedAsRateLimitKey() {
            when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, 5.6.7.8");
            server.expect(requestTo(ASK_URL)).andRespond(withSuccess("""
                    {"result_type":"FALLBACK","answer":"A","matched_question":null,
                     "score":0.1,"suggestions":[]}
                    """, MediaType.APPLICATION_JSON));

            ask("cau hoi");

            verify(chatbotRateLimiterService).tryConsume("1.2.3.4");
        }

        @Test
        void ask_xRealIp_usedWhenNoForwardedFor() {
            when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
            when(httpRequest.getHeader("X-Real-IP")).thenReturn("9.9.9.9");
            server.expect(requestTo(ASK_URL)).andRespond(withSuccess("""
                    {"result_type":"FALLBACK","answer":"A","matched_question":null,
                     "score":0.1,"suggestions":[]}
                    """, MediaType.APPLICATION_JSON));

            ask("cau hoi");

            verify(chatbotRateLimiterService).tryConsume("9.9.9.9");
        }
    }
}
