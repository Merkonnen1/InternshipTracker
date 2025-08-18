// Java
package com.example.InternshipTracker.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GoogleOAuthService {

    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    // Replace with a DB-backed store in production. Key by your app's userId.
    private final Map<String, Credential> credentialsStore = new ConcurrentHashMap<>();

    public String createAuthorizationUrl(String appUserId, String stateToken) throws Exception {
        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        var flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                JSON_FACTORY,
                clientId,
                clientSecret,
                List.of(GmailScopes.GMAIL_READONLY))
                .setAccessType("offline")     // request refresh token
                .build();

        GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setState(URLEncoder.encode(appUserId + ":" + stateToken, StandardCharsets.UTF_8))
                .setAccessType("offline");

        return url.build();
    }

    public void handleOAuthCallback(String appUserId, String code) throws Exception {
        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        var flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                JSON_FACTORY,
                clientId,
                clientSecret,
                List.of(GmailScopes.GMAIL_READONLY))
                .setAccessType("offline")
                .build();

        var tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                httpTransport,
                JSON_FACTORY,
                "https://oauth2.googleapis.com/token",
                clientId,
                clientSecret,
                code,
                redirectUri)
                .execute();

        Credential credential = flow.createAndStoreCredential(tokenResponse, appUserId);
        // Store securely per user (DB/secret vault). Here we use in-memory for brevity.
        credentialsStore.put(appUserId, credential);
    }

    public Gmail getGmailClient(String appUserId) throws Exception {
        Credential credential = credentialsStore.get(appUserId);
        if (credential == null) {
            throw new IllegalStateException("No Gmail credentials found for user " + appUserId);
        }
        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        return new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName("InternshipTracker")
                .build();
    }

    public static String generateStateToken() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}