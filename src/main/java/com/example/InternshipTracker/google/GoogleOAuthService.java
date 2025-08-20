package com.example.InternshipTracker.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import org.springframework.stereotype.Service;
import com.example.InternshipTracker.google.UserGmailTokenRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;


@Service
public class GoogleOAuthService {

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private final UserGmailTokenRepository tokenRepository;

    private final String clientId;
    private final String clientSecret;

    public GoogleOAuthService(UserGmailTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
        this.clientId = mustGetEnv("GOOGLE_CLIENT_ID");
        this.clientSecret = mustGetEnv("GOOGLE_CLIENT_SECRET");
    }

    private static String mustGetEnv(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return v;
    }

    /**
     * Build the Google OAuth authorization URL for the configured client.
     * Includes 'access_type=offline' and 'prompt=consent' to ensure refresh_token issuance.
     */
    public String buildAuthorizationUrl(String redirectUri, String state) throws Exception {
        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        List<String> scopes = List.of(
                "https://www.googleapis.com/auth/gmail.readonly"
        );
        return new GoogleAuthorizationCodeRequestUrl(clientId, redirectUri, scopes)
                .setAccessType("offline")
                .setApprovalPrompt("force") // legacy alias of prompt=consent; Google API client maps it
                .setState(state)
                .build();
    }

    /**
     * Exchange the authorization code for tokens and persist them for the given userId.
     */
    public void exchangeCodeAndStoreTokens(String code, String redirectUri, String userId) throws Exception {
        Objects.requireNonNull(userId, "userId is required");
        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        var tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                httpTransport,
                JSON_FACTORY,
                "https://oauth2.googleapis.com/token",
                clientId,
                clientSecret,
                code,
                redirectUri
        ).execute();

        // Persist refresh token and access token for the user
        var entity = tokenRepository.findByUserId(userId).orElseGet(() -> new UserGmailToken(userId));
        entity.setAccessToken(tokenResponse.getAccessToken());
        entity.setRefreshToken(
                tokenResponse.getRefreshToken() != null && !tokenResponse.getRefreshToken().isBlank()
                        ? tokenResponse.getRefreshToken()
                        : entity.getRefreshToken() // keep existing refresh token if Google didn't return one
        );
        if (tokenResponse.getExpiresInSeconds() != null) {
            entity.setAccessTokenExpiry(Instant.now().plusSeconds(tokenResponse.getExpiresInSeconds()));
        }
        tokenRepository.save(entity);
    }

    /**
     * Build a Credential for the given user using the stored refresh token.
     * No credentials.json file required. If needed, refresh the access token and persist it.
     */
    public Credential getCredentialsForUser(String userId) throws Exception {
        var userToken = tokenRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No Gmail token found for user: " + userId));

        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        var credential = new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientId, clientSecret)
                .build();

        credential.setAccessToken(userToken.getAccessToken());
        credential.setRefreshToken(userToken.getRefreshToken());

        // Refresh if needed and persist
        if (userToken.getAccessTokenExpiry() == null || userToken.getAccessTokenExpiry().isBefore(Instant.now().plusSeconds(60))) {
            boolean refreshed = credential.refreshToken();
            if (!refreshed) {
                throw new IllegalStateException("Failed to refresh Gmail access token for user: " + userId);
            }
            userToken.setAccessToken(credential.getAccessToken());
            if (credential.getExpiresInSeconds() != null) {
                userToken.setAccessTokenExpiry(Instant.now().plusSeconds(credential.getExpiresInSeconds()));
            }
            tokenRepository.save(userToken);
        }

        return credential;
    }

    /**
     * Build Gmail service for the given user.
     */
    public Gmail buildGmailService(String userId) throws Exception {
        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        var credential = getCredentialsForUser(userId);
        return new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName("InternshipTracker")
                .build();
    }

}