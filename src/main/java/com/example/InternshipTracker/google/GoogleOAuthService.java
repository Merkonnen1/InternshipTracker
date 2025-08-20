package com.example.InternshipTracker.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class GoogleOAuthService {

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private final UserGmailTokenRepository tokenRepository;

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    public GoogleOAuthService(UserGmailTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /**
     * Build a Credential for the given user using the stored refresh token.
     * No credentials.json file required. If needed, refresh the access token and persist it.
     */
    public Credential getCredentialsForUser(String userId) throws Exception {
        UserGmailToken userToken = tokenRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No Gmail token found for user: " + userId));

        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setRefreshToken(userToken.getGmailRefreshToken());

        boolean hasStored = userToken.getAccessToken() != null && userToken.getExpiryTime() != null;
        boolean valid = hasStored && userToken.getExpiryTime().isAfter(LocalDateTime.now().plusMinutes(1));

        if (valid) {
            credential.setAccessToken(userToken.getAccessToken());
            return credential;
        }

        if (!credential.refreshToken()) {
            throw new IllegalStateException("Failed to refresh Gmail access token for user: " + userId);
        }

        String newAccessToken = credential.getAccessToken();
        Long expMs = credential.getExpirationTimeMilliseconds();
        LocalDateTime expiry = (expMs != null)
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(expMs), ZoneOffset.UTC)
                : null;

        userToken.setAccessToken(newAccessToken);
        userToken.setExpiryTime(expiry);
        tokenRepository.save(userToken);

        return credential;
    }

    public Gmail buildGmailService(String userId) throws Exception {
        return new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                getCredentialsForUser(userId)
        ).setApplicationName("InternshipTracker").build();
    }
}