package com.example.InternshipTracker.services;

import com.example.InternshipTracker.models.Internship;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class DemoSessionService {
    private final Map<String, Set<Internship>> temporaryDemoData = new HashMap<>();
    private final Set<Internship> presetInternships;

    public DemoSessionService() {
        this.presetInternships = createPresetInternships();
    }

    private Set<Internship> createPresetInternships(){
        Internship google = new Internship();
        google.setId(1);
        google.setCompany("Google");
        google.setPosition("Software Engineer Intern");
        google.setStatus("Under Review");
        google.setDeadline(LocalDate.now().plusMonths(2));
        google.setNotes("Applied through referral");

        Internship microsoft = new Internship();
        microsoft.setId(2);
        microsoft.setCompany("Microsoft");
        microsoft.setPosition("Cloud Developer Intern");
        microsoft.setStatus("Interview Scheduled");
        microsoft.setDeadline(LocalDate.now().plusMonths(1));
        microsoft.setNotes("Technical interview next week");

        Internship amazon = new Internship();
        amazon.setId(3);
        amazon.setCompany("Amazon");
        amazon.setPosition("Backend Developer Intern");
        amazon.setStatus("Pending");
        amazon.setDeadline(LocalDate.now().plusWeeks(3));
        amazon.setNotes("Online assessment completed");
        return new HashSet<>(Arrays.asList(google, microsoft, amazon));
    }

    public Set<Internship> getTemporaryInternships(String sessionId) {
        return temporaryDemoData.computeIfAbsent(sessionId, k -> {
            Set<Internship> sessionInternships = new HashSet<>();
            for (Internship original : presetInternships) {
                Internship copy = new Internship();
                copy.setId(original.getId());
                copy.setCompany(original.getCompany());
                copy.setPosition(original.getPosition());
                copy.setStatus(original.getStatus());
                copy.setDeadline(original.getDeadline());
                copy.setNotes(original.getNotes());
                sessionInternships.add(copy);
            }
            return sessionInternships;
        });
    }

    public void updateInternship(String sessionId, Internship internship) {
        Set<Internship> internships = getTemporaryInternships(sessionId);
        internships.removeIf(i -> i.getId()==(internship.getId()));
        internships.add(internship);
    }

    public void deleteInternship(String sessionId, Internship internship) {
        Set<Internship> internships = getTemporaryInternships(sessionId);
        internships.removeIf(i -> i.getId()==(internship.getId()));
    }

    public void addInternship(String sessionId, Internship internship) {
        Set<Internship> internships = getTemporaryInternships(sessionId);
        int newId = internships.stream()
                .mapToInt(Internship::getId)
                .max()
                .orElse(0) + 1;
        internship.setId(newId);
        internships.add(internship);
    }
    public Internship findInternshipById(String sessionId, int id) {
        return getTemporaryInternships(sessionId).stream()
                .filter(i -> i.getId() == id)
                .findFirst()
                .orElse(null);
    }
    public void clearSession(String sessionId) {
        temporaryDemoData.remove(sessionId);
    }
}