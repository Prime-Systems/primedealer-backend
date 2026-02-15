package com.prime.auth.infrastructure.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prime.auth.application.dto.AuthResponse;
import com.prime.auth.application.service.AuthenticationService;
import com.prime.auth.infrastructure.client.OAuthRegistrationRequest;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthenticationService authenticationService;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String registrationId = extractRegistrationId(request);
        
        log.info("OAuth2 login success: provider={}, user={}", registrationId, oauth2User.getName());
        
        OAuthRegistrationRequest oauthRequest = extractDetails(registrationId, oauth2User.getAttributes());
        
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        
        AuthResponse authResponse = authenticationService.loginWithOAuth(oauthRequest, ipAddress, userAgent);
        
        // Return JSON response (since we are a stateless API)
        // Usually, for OAuth2 client, you might redirect back to the frontend with a token.
        // But if the frontend initiated the flow via the backend, we can return the tokens.
        // For a web-based flow, a redirect is more common: 
        // response.sendRedirect("https://frontend.com/oauth-callback?token=" + authResponse.getAccessToken());
        
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(authResponse));
    }

    private String extractRegistrationId(HttpServletRequest request) {
        // Path is usually /login/oauth2/code/{registrationId}
        String uri = request.getRequestURI();
        if (uri.contains("google")) return "google";
        if (uri.contains("facebook")) return "facebook";
        if (uri.contains("apple")) return "apple";
        return "unknown";
    }

    private OAuthRegistrationRequest extractDetails(String provider, Map<String, Object> attributes) {
        if ("google".equalsIgnoreCase(provider)) {
            return OAuthRegistrationRequest.builder()
                    .provider(provider)
                    .providerUserId((String) attributes.get("sub"))
                    .email((String) attributes.get("email"))
                    .firstName((String) attributes.get("given_name"))
                    .lastName((String) attributes.get("family_name"))
                    .build();
        } else if ("facebook".equalsIgnoreCase(provider)) {
            return OAuthRegistrationRequest.builder()
                    .provider(provider)
                    .providerUserId((String) attributes.get("id"))
                    .email((String) attributes.get("email"))
                    .firstName((String) attributes.get("first_name"))
                    .lastName((String) attributes.get("last_name"))
                    .build();
        } else if ("apple".equalsIgnoreCase(provider)) {
            return OAuthRegistrationRequest.builder()
                    .provider(provider)
                    .providerUserId((String) attributes.get("sub"))
                    .email((String) attributes.get("email"))
                    .build();
        }
        return OAuthRegistrationRequest.builder()
                .provider(provider)
                .build();
    }
}
