package fpt.capstone.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class Congiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(
                        auth -> auth
                                .requestMatchers("/","/login", "/oauth2/**").permitAll()
                                .anyRequest().authenticated()
                )// xac dinh endpoint
                .formLogin(Customizer.withDefaults())// username + pass
                .oauth2Login(Customizer.withDefaults());// gg
           return http.build();
    }
}
