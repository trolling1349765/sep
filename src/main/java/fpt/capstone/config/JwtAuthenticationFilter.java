package fpt.capstone.config;

import fpt.capstone.entity.User;
import fpt.capstone.repository.UserRepository;
import fpt.capstone.service.PermissionCacheService;
import fpt.capstone.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PermissionCacheService permissionCacheService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String accessToken = extractAccessToken(request);

        if (accessToken != null && jwtUtil.isTokenValid(accessToken) && !jwtUtil.isTokenExpired(accessToken)) {
            try {
                String userId = jwtUtil.extractUserId(accessToken);
                User user = userRepository.findWithRoleById(userId).orElse(null);

                if (user != null) {
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    if (user.getRole() != null) {
                        authorities.add(new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().getName().toUpperCase()));
                        for (String code : permissionCacheService.getRightCodes(user.getRole().getId())) {
                            authorities.add(new SimpleGrantedAuthority(code));
                        }
                    } else {
                        log.warn("User {} has no role assigned; proceeding with no authorities", userId);
                    }

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user,
                            null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                log.error("Error authenticating JWT token: {}", e.getMessage());
                throw new ServletException(e);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extractAccessToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }
}