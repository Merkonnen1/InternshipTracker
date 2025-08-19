package com.example.InternshipTracker.google;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;

@Controller
public class GoogleOAuthCallbackController {

    private final GoogleOAuthService googleOAuthService;

    public GoogleOAuthCallbackController(GoogleOAuthService googleOAuthService) {
        this.googleOAuthService = googleOAuthService;
    }

    @GetMapping("/oauth2/callback/google")
    public String handleCallback(@RequestParam String code,
                                 @RequestParam String state,
                                 HttpSession session) {
        try {
            // Extract your app's user ID from the state parameter
            String[] stateParts = state.split(":");
            String appUserId = stateParts[0]; // Your app's user ID
            String stateToken = stateParts[1]; // The security token

            // Exchange the authorization code for tokens
            googleOAuthService.handleOAuthCallback(appUserId, code);

            // Store success in session
            session.setAttribute("gmailConnected", true);

            return "redirect:/dashboard?gmailConnected=true";

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/dashboard?gmailError=true";
        }
    }
}