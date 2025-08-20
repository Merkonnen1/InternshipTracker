//package com.example.InternshipTracker.google;
//
//import jakarta.persistence.*;
//import java.time.Instant;
//
//@Entity
//@Table(
//        name = "user_gmail_tokens",
//        uniqueConstraints = @UniqueConstraint(name = "uk_user_gmail_tokens_user_id", columnNames = "user_id")
//)
//public class UserGmailToken {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "user_id", nullable = false, length = 191)
//    private String userId;
//
//    @Column(name = "access_token", length = 2048)
//    private String accessToken;
//
//    @Column(name = "refresh_token", length = 2048)
//    private String refreshToken;
//
//    @Column(name = "token_expiry")
//    private Instant tokenExpiry;
//
//    @Column(name = "token_type")
//    private String tokenType;
//
//    @Column(name = "scope", length = 2048)
//    private String scope;
//
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private Instant createdAt;
//
//    public UserGmailToken() {
//    }
//
//    public UserGmailToken(String userId) {
//        this.userId = userId;
//    }
//
//    @PrePersist
//    void prePersist() {
//        if (this.createdAt == null) {
//            this.createdAt = Instant.now();
//        }
//    }
//
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public String getUserId() {
//        return userId;
//    }
//
//    public void setUserId(String userId) {
//        this.userId = userId;
//    }
//
//    public String getAccessToken() {
//        return accessToken;
//    }
//
//    public void setAccessToken(String accessToken) {
//        this.accessToken = accessToken;
//    }
//
//    public String getRefreshToken() {
//        return refreshToken;
//    }
//
//    public void setRefreshToken(String refreshToken) {
//        this.refreshToken = refreshToken;
//    }
//
//    public Instant getTokenExpiry() {
//        return tokenExpiry;
//    }
//
//    public void setTokenExpiry(Instant tokenExpiry) {
//        this.tokenExpiry = tokenExpiry;
//    }
//
//    // Alias accessors to match callers expecting "accessTokenExpiry"
//    public Instant getAccessTokenExpiry() {
//        return tokenExpiry;
//    }
//
//    public void setAccessTokenExpiry(Instant accessTokenExpiry) {
//        this.tokenExpiry = accessTokenExpiry;
//    }
//
//    public String getTokenType() {
//        return tokenType;
//    }
//
//    public void setTokenType(String tokenType) {
//        this.tokenType = tokenType;
//    }
//
//    public String getScope() {
//        return scope;
//    }
//
//    public void setScope(String scope) {
//        this.scope = scope;
//    }
//
//    public Instant getCreatedAt() {
//        return createdAt;
//    }
//
//    public void setCreatedAt(Instant createdAt) {
//        this.createdAt = createdAt;
//    }
//}