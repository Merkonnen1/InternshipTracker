package com.example.InternshipTracker.Controllers;

import com.example.InternshipTracker.models.User;
import com.example.InternshipTracker.models.UserDto;
import com.example.InternshipTracker.repositories.UserRepository;
import com.example.InternshipTracker.services.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {
    private final UserRepository userRepository;
    private final UserService userService;

    public AuthController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new UserDto());
        return "register";
    }

    @GetMapping("/dashboard")
    public String listInternships(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("internships", user.getInternships());
        model.addAttribute("user",user);
        return "dashboard/internship-list";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") UserDto userDto,
                             BindingResult result) {
        if (result.hasErrors()) {
            return "register";
        }

        userService.registerUser(userDto);
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }
}