package com.example.InternshipTracker.google;

import com.example.InternshipTracker.models.User;
import com.example.InternshipTracker.repositories.UserRepository;
import com.example.InternshipTracker.services.InternshipService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
public class GoogleAuthController {

    private final InternshipService internshipService;
    private final GoogleOAuthService googleOAuthService;
    private final UserRepository userRepository;

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    public GoogleAuthController(GoogleOAuthService googleOAuthService,
                                InternshipService internshipService,
                                UserRepository userRepository) {
        this.googleOAuthService = googleOAuthService;
        this.internshipService = internshipService;
        this.userRepository = userRepository;
    }

    @GetMapping("/gmail/connect")
    public RedirectView connect(HttpSession session) {
        User currentUser = getCurrentUserOrThrow();
        // CSRF-safe state includes the user id and a random nonce
        String state = currentUser.getId() + ":" + UUID.randomUUID();

        session.setAttribute("oauth2_state", state);

        List<String> scopes = List.of(
                "https://www.googleapis.com/auth/gmail.readonly",
                "email",
                "profile"
        );

        String authorizationUrl = UriComponentsBuilder
                .fromHttpUrl("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", String.join(" ", scopes))
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("state", state)
                .build(true)
                .toUriString();

        RedirectView rv = new RedirectView(authorizationUrl);
        rv.setExposeModelAttributes(false);
        return rv;
    }

    @GetMapping("/oauth2/callback/google")
    public RedirectView callback(@RequestParam(name = "code", required = false) String code,
                                 @RequestParam(name = "state", required = false) String state,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        String expectedState = (String) session.getAttribute("oauth2_state");
        if (code == null || state == null || expectedState == null || !expectedState.equals(state)) {
            redirectAttributes.addFlashAttribute("error", "Invalid OAuth state or missing code.");
            return new RedirectView("/");
        }

        // One-time use state
        session.removeAttribute("oauth2_state");

        try {
            // Optionally keep the code in session if you need to exchange it elsewhere
            session.setAttribute("google_auth_code", code);

            // Mark the current user as connected
            User currentUser = getCurrentUserOrThrow();
            if (!currentUser.isGmailConnected()) {
                currentUser.setGmailConnected(true);
                userRepository.save(currentUser);
            }

            redirectAttributes.addFlashAttribute("success", "Gmail connected successfully.");
            return new RedirectView("/dashboard");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Failed to complete Google OAuth: " + ex.getMessage());
            return new RedirectView("/");
        }
    }

    @PostMapping("/gmail/disconnect")
    public RedirectView disconnect(RedirectAttributes redirectAttributes) {
        User user = getCurrentUserOrThrow();
        if (user.isGmailConnected()) {
            user.setGmailConnected(false);
            userRepository.save(user);
        }
        redirectAttributes.addFlashAttribute("success", "Gmail disconnected.");
        return new RedirectView("/dashboard");
    }

    @GetMapping("/gmail/check")
    public RedirectView checkEmails(HttpSession session) {
        try {
            User user = getCurrentUserOrThrow();
            // Validate we can build a Gmail service for this user (throws if not configured)
            googleOAuthService.buildGmailService(String.valueOf(user.getId()));
            return new RedirectView("/dashboard");
        } catch (Exception e) {
            return new RedirectView("/");
        }
    }

    private User getCurrentUserOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("Not authenticated");
        }
        String email = authentication.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        return userOpt.orElseThrow(() -> new IllegalStateException("User not found"));
    }
}