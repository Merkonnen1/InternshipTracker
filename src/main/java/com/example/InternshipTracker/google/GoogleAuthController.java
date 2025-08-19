// Java
package com.example.InternshipTracker.google;

import ch.qos.logback.core.model.Model;
import com.example.InternshipTracker.models.Internship;
import com.example.InternshipTracker.models.User;
import com.example.InternshipTracker.repositories.UserRepository;
import com.example.InternshipTracker.services.InternshipService;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.origin.SystemEnvironmentOrigin;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import com.example.InternshipTracker.services.InternshipService;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Controller
public class GoogleAuthController {
    private final InternshipService internshipService;

    private final GoogleOAuthService googleOAuthService;
    private final UserRepository userRepository;

    public GoogleAuthController(GoogleOAuthService googleOAuthService, InternshipService internshipService, UserRepository userRepository) {
        this.googleOAuthService = googleOAuthService;
        this.internshipService = internshipService;
        this.userRepository = userRepository;
    }

    @GetMapping("/gmail/connect")
    public String connect(HttpSession session) throws Exception {
        try {
            // Get your app's user ID from session or authentication
            String appUserId = internshipService.getCurrentUser().getId().toString();
            String stateToken = GoogleOAuthService.generateStateToken();

            // Store state token in session for validation
            session.setAttribute("oauthState", stateToken);

            String authUrl = googleOAuthService.createAuthorizationUrl(appUserId, stateToken);
            return "redirect:" + authUrl;

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/dashboard";
        }
    }

    // Step 2: Google redirects here with ?code=...&state=...
//    @GetMapping("/oauth2/callback/google")
//    public RedirectView callback(@RequestParam String code,
//                                 @RequestParam String state,
//                                 HttpSession session,
//                                RedirectAttributes redirectAttributes) throws Exception {
//        String expectedState = (String) session.getAttribute("oauth_state");
//        if (expectedState == null || !state.contains(expectedState)) {
//            return new RedirectView("/error?reason=state_mismatch");
//        }
//        User user = internshipService.getCurrentUser();
//        String appUserId = internshipService.getCurrentUser().getId().toString();
//        user.setGmailConnected(true);
//        userRepository.save(user); // persist in DB
//        googleOAuthService.handleOAuthCallback(appUserId, code);
//        redirectAttributes.addFlashAttribute("connected", true);
//        return new RedirectView("/dashboard");
//    }
    @GetMapping("/oauth2/callback/google")
    public String handleCallback(@RequestParam String code,
                                 @RequestParam String state,
                                 HttpSession session) {
        try {
            // URL decode the state parameter first
            String decodedState = URLDecoder.decode(state, StandardCharsets.UTF_8);
            String[] stateParts = decodedState.split(":");

            String appUserId = stateParts[0]; // Your app's user ID
            String stateToken = stateParts[1]; // Security token

            // Validate state token (compare with what you stored in session)
            String storedState = (String) session.getAttribute("oauthState");
            if (!stateToken.equals(storedState)) {
                return "redirect:/dashboard";
            }

            // Exchange authorization code for access token
            googleOAuthService.handleOAuthCallback(appUserId, code);

            // Clear the state token from session
            session.removeAttribute("oauthState");

            return "redirect:/dashboard";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/dashboard";
        }
    }
    @PostMapping("/gmail/disconnect")
    public RedirectView disconnect() {
        User user = internshipService.getCurrentUser();
        user.setGmailConnected(false);
        userRepository.save(user);
        return new RedirectView("/dashboard");
    }
    @GetMapping("/gmail/check")
    public RedirectView checkEmails(HttpSession session) throws Exception {
        String appUserId = internshipService.getCurrentUser().getId().toString();
        GmailQuickstart gmailQuickstart = new GmailQuickstart();
        List<JsonObject> ans = gmailQuickstart.fetchEmailsWithAi();
        for (JsonObject i:ans){
            Internship internship = new Internship();
            JsonElement company = i.get("company");
            JsonElement position = i.get("position");
            JsonElement status = i.get("status");
            JsonElement tmp = i.get("deadline");
            if (!company.isJsonNull() && !position.isJsonNull() && !status.isJsonNull()) {
                internship.setCompany(company.getAsString());
                internship.setPosition(position.getAsString());
                internship.setStatus(status.getAsString());

                if (!tmp.isJsonNull()) {
                    internship.setDeadline(LocalDate.parse(tmp.getAsString()));
                }

                JsonElement notes = i.get("notes");
                internship.setNotes(notes != null && !notes.isJsonNull() ? notes.getAsString() : null);

                internshipService.saveInternship(internship);
            }
        }
        System.out.println(ans);
        return new RedirectView("/dashboard"); 
    }
}