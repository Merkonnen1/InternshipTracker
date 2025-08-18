package com.example.InternshipTracker.services;

import com.example.InternshipTracker.models.Internship;
import com.example.InternshipTracker.models.User;
import com.example.InternshipTracker.repositories.InternshipRepository;
import com.example.InternshipTracker.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InternshipService {
    private final InternshipRepository internshipRepository;
    private final UserRepository userRepository;

    public InternshipService(InternshipRepository internshipRepository, UserRepository userRepository) {
        this.internshipRepository = internshipRepository;
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<Internship> getCurrentUserInternships() {
        return internshipRepository.findByUserOrderByDeadlineAsc(getCurrentUser());
    }

    public void saveInternship(Internship internship) {
        internship.setUser(getCurrentUser());
        internshipRepository.save(internship);
    }

    public Internship getInternshipById(int id) {
        return internshipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Internship not found"));
    }

    public void deleteInternship(int id) {
        Internship internship = getInternshipById(id);
        // Security check - ensure user owns this internship
        if (internship.getUser().getId().equals(getCurrentUser().getId())) {
            internshipRepository.delete(internship);
        } else {
            throw new RuntimeException("Not authorized to delete this internship");
        }
    }
}