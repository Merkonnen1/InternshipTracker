//package com.example.InternshipTracker.google;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import java.util.Optional;
//
//public interface UserGmailTokenRepository extends JpaRepository<UserGmailToken, Long> {
//    Optional<UserGmailToken> findByUserId(String userId);
//    boolean existsByUserId(String userId);
//    void deleteByUserId(String userId);
//
//}