package net.javaguides.identity_service.service.impl;

import net.javaguides.identity_service.exception.AuthException;
import net.javaguides.identity_service.service.GoogleOAuth2Service;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GoogleOAuth2ServiceImpl implements GoogleOAuth2Service {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleOAuth2ServiceImpl.class);

    private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final RestClient restClient = RestClient.create();
    private final JwtDecoder idTokenDecoder;

    public GoogleOAuth2ServiceImpl(@Value("${google.oauth2.client-id:}") String clientId,
                                   @Value("${google.oauth2.client-secret:}") String clientSecret,
                                   @Value("${google.oauth2.redirect-uri:postmessage}") String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.idTokenDecoder = buildIdTokenDecoder(clientId);
    }

    @Override
    public Jwt exchangeCodeForIdToken(String code) {
        if (StringUtils.isEmpty(clientId) || StringUtils.isEmpty(clientSecret)) {
            throw new AuthException("Google login is not configured");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        Map<String, Object> tokenResponse;
        try {
            tokenResponse = restClient.post()
                    .uri(TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException e) {
            LOGGER.warn("Exchange Google authorization code failed: {}", e.getMessage());
            throw new AuthException("Invalid Google authorization code");
        }

        Object idToken = tokenResponse == null ? null : tokenResponse.get("id_token");
        if (idToken == null) {
            throw new AuthException("Google did not return an ID token");
        }

        try {
            return idTokenDecoder.decode(idToken.toString());
        } catch (JwtException e) {
            LOGGER.warn("Invalid Google ID token: {}", e.getMessage());
            throw new AuthException("Invalid Google ID token");
        }
    }

    /**
     * Decoder verifying signature (Google JWKS), expiry, issuer and audience (our client id).
     */
    private static JwtDecoder buildIdTokenDecoder(String clientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build();
        JwtClaimValidator<Object> issuerValidator = new JwtClaimValidator<>(
                JwtClaimNames.ISS, iss -> iss != null && GOOGLE_ISSUERS.contains(iss.toString()));
        JwtClaimValidator<List<String>> audienceValidator = new JwtClaimValidator<>(
                JwtClaimNames.AUD, aud -> aud != null && aud.contains(clientId));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), issuerValidator, audienceValidator));
        return decoder;
    }
}
