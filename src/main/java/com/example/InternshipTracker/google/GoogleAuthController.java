//// Java
//package com.example.InternshipTracker.google;
//
//import com.example.InternshipTracker.models.User;
//import com.example.InternshipTracker.repositories.UserRepository;
//import com.google.api.services.gmail.Gmail;
//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//import com.google.gson.JsonObject;
//import jakarta.servlet.http.HttpSession;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.servlet.view.RedirectView;
//
//import java.security.SecureRandom;
//import java.util.Base64;
//import java.util.Optional;
//
//@Controller
//public class GoogleAuthController {
//
//    private final UserRepository userRepository;
//    private final GoogleOAuthService googleOAuthService;
//
//    private final String clientId = mustGetEnv("GOOGLE_CLIENT_ID");
//    private final String clientSecret = mustGetEnv("GOOGLE_CLIENT_SECRET");
//    private final String redirectUri = mustGetEnv("GOOGLE_REDIRECT_URI");
//
//    public GoogleAuthController(UserRepository userRepository, GoogleOAuthService googleOAuthService) {
//        this.userRepository = userRepository;
//        this.googleOAuthService = googleOAuthService;
//    }
//
//    private static String mustGetEnv(String name) {
//        String v = System.getenv(name);
//        if (v == null || v.isBlank()) {
//            throw new IllegalStateException("Missing required environment variable: " + name);
//        }
//        return v;
//    }
//
//    private static String randomState() {
//        byte[] bytes = new byte[24];
//        new SecureRandom().nextBytes(bytes);
//        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
//    }
//
//    /**
//     * Start Google OAuth flow.
//     * Expects the current user to be present in session under "userEmail".
//     */
//    @GetMapping("/oauth2/google")
//    public RedirectView startGoogleOAuth(HttpSession session) throws Exception {
//        String state = randomState();
//        session.setAttribute("oauth_state", state);
//        String url = googleOAuthService.buildAuthorizationUrl(redirectUri, state);
//        return new RedirectView(url);
//    }
//
//    /**
//     * Handle Google OAuth callback.
//     * Stores tokens for the currently logged-in user and marks Gmail connected.
//     */
//    @GetMapping("/oauth2/callback/google")
//    public RedirectView handleGoogleCallback(@RequestParam("code") String code,
//                                             @RequestParam("state") String state,
//                                             HttpSession session) throws Exception {
//        String expected = (String) session.getAttribute("oauth_state");
//        session.removeAttribute("oauth_state");
//        if (expected == null || !expected.equals(state)) {
//            throw new IllegalStateException("Invalid OAuth state");
//        }
//
//        // Resolve the current user from the session
//        String userEmail = (String) session.getAttribute("userEmail"); // set this at login
//        if (userEmail == null || userEmail.isBlank()) {
//            throw new IllegalStateException("No logged-in user context in session");
//        }
//
//        Optional<User> userOpt = userRepository.findByEmail(userEmail);
//        User user = userOpt.orElseThrow(() -> new IllegalStateException("User not found for email: " + userEmail));
//
//        // Exchange code and persist tokens
//        googleOAuthService.exchangeCodeAndStoreTokens(code, redirectUri, String.valueOf(user.getId()));
//
//        // Mark user as Gmail connected if your User entity supports it
//        user.setGmailConnected(true);
//        userRepository.save(user);
//
//        // Redirect back to your dashboard or success page
//        return new RedirectView("/dashboard");
//    }
//
//    /**
//     * Fetch the latest email and analyze it with AI.
//     * Returns JSON with fields: company, position, status, contact, important_dates.
//     */
//    @GetMapping("/gmail/fetch-latest")
//    public ResponseEntity<String> fetchLatestAndAnalyze(HttpSession session) throws Exception {
//        String userEmail = (String) session.getAttribute("userEmail");
//        if (userEmail == null || userEmail.isBlank()) {
//            return ResponseEntity.badRequest().body("{\"error\":\"No logged-in user context\"}");
//        }
//        Optional<User> userOpt = userRepository.findByEmail(userEmail);
//        if (userOpt.isEmpty()) {
//            return ResponseEntity.badRequest().body("{\"error\":\"User not found\"}");
//        }
//        String userId = String.valueOf(userOpt.get().getId());
//
//        Gmail gmail = googleOAuthService.buildGmailService(userId);
//        String body = GmailQuickstart.fetchLatestEmailPlainText(gmail);
//        if (body.isBlank()) {
//            return ResponseEntity.ok("{\"message\":\"No recent emails found\"}");
//        }
//
//        String apiKey = mustGetEnv("OPENAI_API_KEY");
//        JsonObject result = GmailQuickstart.extractJobInfoWithAI(body, apiKey);
//        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
//        return ResponseEntity.ok(gson.toJson(result));
//    }
//}