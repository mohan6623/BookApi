package com.marvel.springsecurity.model;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;


//@Entity
@Data
@NoArgsConstructor
//@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    @Column(nullable = false, unique = true)
    private String username;

    private String password;

    private String role;


    @Column(nullable = false , unique = true)
    private String mail;

    @Column(nullable = false)
    private String firstName;

    private String lastName;

    private String imageUrl;

    private String imagePublicId;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer roleVersion = 0;

    // OAuth2 provider information
    // LOCAL = normal registration; GOOGLE / GITHUB = OAuth providers
    @Column(nullable = false, length = 20)
    private String provider = "LOCAL";

    // ID from the OAuth provider (e.g., Google sub, GitHub id)
    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "email_verified", nullable = false, columnDefinition = "boolean default false")
    private Boolean emailVerified = false;

    private Instant createdAt;

    private Instant updatedAt;

    public User(Integer userId, String username, String password, String role, String mail, String imagePublicId, String imageUrl, Integer roleVersion) {
        this.userId = userId;
        this.username = username.toLowerCase();
        this.password = password;
        this.role = role;
        this.mail = mail.toLowerCase();
        this.imagePublicId = imagePublicId;
        this.imageUrl = imageUrl;
        this.roleVersion = roleVersion;
    }

    public void setUsername(String username) {
        this.username = username == null ? null : username.toLowerCase();
    }

    public void setMail(String mail) {
        this.mail = mail == null ? null : mail.toLowerCase();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = java.time.Instant.now();
    }
}