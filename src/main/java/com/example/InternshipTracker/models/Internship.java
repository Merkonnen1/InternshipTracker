package com.example.InternshipTracker.models;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="internship")
public class Internship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String company;
    private String position;
    private String status;
    private LocalDate deadline;
    private String notes;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Add getter and setter for user
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
