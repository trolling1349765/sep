package fpt.capstone.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/capstone/auth/captcha").permitAll()
                        .requestMatchers(HttpMethod.POST, "/capstone/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/capstone/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/capstone/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/capstone/auth/password-reset/request").permitAll()
                        .requestMatchers(HttpMethod.POST, "/capstone/auth/password-reset/confirm").permitAll()
                        .requestMatchers(HttpMethod.GET, "/capstone/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/capstone/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.POST, "/capstone/auth/logout-all").authenticated()
                        .requestMatchers(HttpMethod.POST, "/capstone/auth/change-password").authenticated()
                        .requestMatchers(HttpMethod.GET, "/capstone/users/profile").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/capstone/users/profile").authenticated()
                        .requestMatchers("/capstone/admin/**").hasRole("ADMIN")
                        .requestMatchers("/capstone/notifications/**").authenticated()
                        .requestMatchers("/capstone/support-requests/**").authenticated()
                        .requestMatchers("/capstone/support-requests/manage")
                        .hasAnyRole("RECEPTION_OFFICER", "SOCIAL_AFFAIRS_OFFICER")
                        .requestMatchers("/capstone/support-requests/*/reply")
                        .hasAnyRole("RECEPTION_OFFICER", "SOCIAL_AFFAIRS_OFFICER")
                        .requestMatchers("/capstone/support-requests/*/status")
                        .hasAnyRole("RECEPTION_OFFICER", "SOCIAL_AFFAIRS_OFFICER")
                        .requestMatchers(HttpMethod.GET, "/capstone/provinces").permitAll()
                        .requestMatchers(HttpMethod.GET, "/capstone/provinces/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/capstone/provinces/{provinceCode}/wards").permitAll()
                        .requestMatchers(HttpMethod.GET, "/capstone/citizen-portal/policies/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/capstone/citizen-portal/chatbot/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/applications/**").hasAnyRole("USER", "OFF1")
                        .requestMatchers(HttpMethod.POST, "/applications/**").hasRole("USER")
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
