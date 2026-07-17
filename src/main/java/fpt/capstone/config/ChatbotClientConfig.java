package fpt.capstone.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class ChatbotClientConfig {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    /**
     * chatbot.service.url has NO default on purpose: it must be declared in
     * application-*.properties so a missing configuration fails at startup
     * instead of silently pointing at the wrong host.
     */
    @Bean
    public RestClient chatbotRestClient(@Value("${chatbot.service.url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT);
        factory.setReadTimeout(TIMEOUT);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}
