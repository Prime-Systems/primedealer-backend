package com.prime.auth.infrastructure.oauth2;

import com.prime.auth.infrastructure.client.OAuthRegistrationRequest;
import com.prime.auth.infrastructure.client.UserInfo;
import com.prime.auth.infrastructure.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserServiceClient userServiceClient;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        log.info("Loaded OAuth2 user from {}: {}", registrationId, attributes);
        
        OAuthRegistrationRequest request = extractDetails(registrationId, attributes);
        
        // We don't necessarily need to call user-service HERE because the success handler
        // will need to do it anyway to get the full UserInfo for session creation.
        // But we could validate/pre-register here. 
        // For simplicity in this flow, we'll return a custom OAuth2User or just the standard one
        // and handle the logic in the SuccessHandler.
        
        return oauth2User;
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
            // Apple attributes are slightly different and often in the 'id_token'
            return OAuthRegistrationRequest.builder()
                    .provider(provider)
                    .providerUserId((String) attributes.get("sub"))
                    .email((String) attributes.get("email"))
                    .build();
        }
        
        throw new OAuth2AuthenticationException("Unsupported provider: " + provider);
    }
}
