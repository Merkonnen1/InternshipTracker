package com.example.InternshipTracker.google;

import com.example.InternshipTracker.models.User;
import com.example.InternshipTracker.repositories.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Controller
public class GoogleAuthController {

    private final UserRepository userRepository;

    @Value("${google.oauth.client-id:YOUR_GOOGLE_CLIENT_ID}")
    private String clientId;

    @Value("${google.oauth.client-secret:YOUR_GOOGLE_CLIENT_SECRET}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri:http://localhost:8080/oauth2/callback/google}")
    private String redirectUri;

    public GoogleAuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/gmail/connect")
    public RedirectView connect(HttpSession session) {
        // Resolve current user
        String email = resolveSessionEmail(session);
        if (email == null) {
            return new RedirectView("/login?error=not_authenticated");
        }

        // Short-circuit if already connected
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent() && userOpt.get().isGmailConnected()) {
            return new RedirectView("/?info=gmail_already_connected");
        }

        // CSRF protection via state
        String state = UUID.randomUUID().toString();
        session.setAttribute("google_oauth_state", state);

        URI authUri = UriComponentsBuilder
                .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", String.join(" ",
                        "openid",
                        "email",
                        "profile",
                        "https://www.googleapis.com/auth/gmail.readonly"))
                .queryParam("access_type", "offline")
                .queryParam("include_granted_scopes", "true")
                .queryParam("state", state)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        return new RedirectView(authUri.toString());
    }

    @GetMapping("/oauth2/callback/google")
    public RedirectView callback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // Validate state
        String expectedState = (String) session.getAttribute("google_oauth_state");
        session.removeAttribute("google_oauth_state");
        if (expectedState == null || state == null || !expectedState.equals(state)) {
            redirectAttributes.addFlashAttribute("error", "Invalid OAuth state.");
            return new RedirectView("/?error=invalid_state");
        }

        if (code == null || code.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Missing authorization code.");
            return new RedirectView("/?error=missing_code");
        }

        // Exchange authorization code for tokens (placeholder; implement HTTP POST to Google's token endpoint)
        session.setAttribute("gmail_access_token", "<ACCESS_TOKEN_PLACEHOLDER>");
        session.setAttribute("gmail_refresh_token", "<REFRESH_TOKEN_PLACEHOLDER>");

        // Mark the user as connected
        String email = resolveSessionEmail(session);
        if (email != null) {
            userRepository.findByEmail(email).ifPresent(user -> {
                user.setGmailConnected(true);
                userRepository.save(user);
            });
        } else {
            redirectAttributes.addFlashAttribute("warning", "Connected to Google, but user session was not found.");
        }

        redirectAttributes.addFlashAttribute("success", "Gmail successfully connected.");
        return new RedirectView("/?success=gmail_connected");
    }

    @PostMapping("/gmail/disconnect")
    public RedirectView disconnect(RedirectAttributes redirectAttributes) {
        // Access current session without changing method signature
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpSession session = (attrs != null) ? attrs.getRequest().getSession(false) : null;

        String email = resolveSessionEmail(session);
        if (email != null) {
            userRepository.findByEmail(email).ifPresent(user -> {
                user.setGmailConnected(false);
                userRepository.save(user);
            });
        }

        // Clear session-stored token placeholders if present
        if (session != null) {
            session.removeAttribute("gmail_access_token");
            session.removeAttribute("gmail_refresh_token");
        }

        // Placeholder: Revoke tokens using Google's revocation endpoint if persisted
        redirectAttributes.addFlashAttribute("success", "Gmail disconnected.");
        return new RedirectView("/?success=gmail_disconnected");
    }

    @GetMapping("/gmail/check")
    public RedirectView checkEmails(HttpSession session) {
        String email = resolveSessionEmail(session);
        if (email == null) {
            return new RedirectView("/login?error=not_authenticated");
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty() || !userOpt.get().isGmailConnected()) {
            return new RedirectView("/?error=gmail_not_connected");
        }

        if (session.getAttribute("gmail_access_token") == null) {
            return new RedirectView("/?error=missing_token");
        }

        // Placeholder: Call Gmail API with the access token to check emails
        return new RedirectView("/?info=checked_emails");
    }

    private String resolveSessionEmail(HttpSession session) {
        if (session == null) return null;
        Object v = session.getAttribute("userEmail");
        if (!(v instanceof String s) || s.isBlank()) {
            v = session.getAttribute("email");
            if (!(v instanceof String s2) || s2.isBlank()) {
                return null;
            }
            return (String) v;
        }
        return (String) v;
    }
}