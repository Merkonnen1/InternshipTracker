package com.example.InternshipTracker.services;

import com.example.InternshipTracker.models.User;
import com.example.InternshipTracker.models.UserDto;
import com.example.InternshipTracker.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void registerUser(UserDto userDto) {
        var email = userDto.getEmail().trim().toLowerCase();

        userRepository.findByEmail(email).ifPresent(u -> {
            throw new IllegalArgumentException("Email already registered");
        });

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setFirstname(userDto.getFirstname());
        user.setLastname(userDto.getLastname());
        userRepository.save(user);
    }
}