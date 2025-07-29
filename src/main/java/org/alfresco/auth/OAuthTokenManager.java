package org.alfresco.auth;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * <p>Light‑weight helper around the OAuth2 <strong>Client‑Credentials</strong> flow.</p>
 *
 * <p>The class maintains an in‑memory cache and only contacts the IdP when the cached token is
 * absent or expired. It is thread‑safe and intentionally free of external caching libraries.</p>
 *
 * <h2>Caveats</h2>
 * <ul>
 *   <li>Tokens are stored in JVM memory only — they are lost after restart.</li>
 *   <li>No refresh‑token logic is implemented because the Client‑Credentials flow does not
 *       issue refresh tokens.</li>
 * </ul>
 */
public class OAuthTokenManager {

    private static final Log LOGGER = LogFactory.getLog(OAuthTokenManager.class);
    private static final long SKEW_SECONDS = 10;

    private String clientId;
    private String clientSecret;
    private String oauthUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private volatile CachedToken cachedToken = new CachedToken(null, 0);

    /**
     * Returns a valid access‑token string (never {@code null} or blank).
     * <p>The method is almost free when the token is cached; it performs a single HTTP
     * round‑trip only when a refresh is required.</p>
     */
    public String getAccessToken() {
        CachedToken snapshot = cachedToken;
        if (snapshot.isValid()) {
            return snapshot.value();
        }

        synchronized (this) {
            snapshot = cachedToken;
            if (snapshot.isValid()) {
                return snapshot.value();
            }
            return fetchNewToken();
        }
    }

    /**
     * Requests a new OAuth2 access token using client credentials flow with Basic Auth.
     *
     * @return the access token string
     * @throws RuntimeException if token request fails or response is invalid
     */
    private String fetchNewToken() {
        String tokenUrl = oauthUrl + "/connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        String credentials = clientId + ":" + clientSecret;
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedCredentials);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    tokenUrl, HttpMethod.POST, request, new ParameterizedTypeReference<>() {}
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to obtain access token. HTTP Status: " +
                        response.getStatusCode());
            }

            Map<String, Object> body = response.getBody();
            String token = body.get("access_token").toString().trim();
            Number expiresIn = (Number) body.getOrDefault("expires_in", 900);
            cacheToken(token, Duration.ofSeconds(expiresIn.longValue()));

            return token;

        } catch (Exception e) {
            LOGGER.error("Error requesting OAuth token: " + e.getMessage());
            throw new RuntimeException("Failed to request OAuth token", e);
        }
    }

    /**
     * Stores the token in cache with calculated expiration time.
     *
     * @param token the access token to cache
     * @param expiresIn number of seconds for expiration
     */
    private void cacheToken(String token, Duration expiresIn) {
        long expiryMillis = System.currentTimeMillis() +
                Math.max(0, expiresIn.minusSeconds(SKEW_SECONDS).toMillis());
        cachedToken = new CachedToken(token, expiryMillis);
    }

    /**
     * Immutable value object that carries a token and the epoch‑millis timestamp of its expiry.
     *
     * @param value the OAuth2 access token
     * @param expiryEpochMillis timestamp in milliseconds when token expires
     */
    private record CachedToken(String value, long expiryEpochMillis) {
        private static final CachedToken EMPTY = new CachedToken("", 0);

        boolean isValid() {
            return System.currentTimeMillis() < expiryEpochMillis && !value.isBlank();
        }
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setOauthUrl(String oauthUrl) {
        this.oauthUrl = oauthUrl;
    }

}
