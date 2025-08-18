package com.example.InternshipTracker.repositories;

import com.example.InternshipTracker.models.Internship;
import com.example.InternshipTracker.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InternshipRepository extends JpaRepository<Internship,Integer> {
    List<Internship> findByUser(User user);
    List<Internship> findByUserOrderByDeadlineAsc(User user);
}
