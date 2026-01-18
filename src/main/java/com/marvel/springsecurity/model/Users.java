package com.marvel.springsecurity.model;

import com.marvel.springsecurity.dto.UserDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
//@Table(indexes = @Index(name = "idx_mail", columnList = "email"))
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "users")
public class Users {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    @Column(nullable = false, length = 100)
    private String username;
    private String password;

//    @Column(nullable = false)
    private String name;

    private String imageUrl;
    private String imagePublicId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    private String secondaryEmail;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean emailVerified;

    private String verificationToken;

    // TODO: [MEDIUM TERM] Add verification attempt counter field
    // private Integer verificationAttempts = 0;
    // Track failed verification attempts, lock after 3 failed attempts
    // Reset counter on successful verification or new token generation

    // TODO: [MEDIUM TERM] Add verification token expiry timestamp field
    // private Instant verificationTokenExpiry;
    // Store explicit expiry time for better error messages

    // TODO: [LONG TERM] Add 2FA fields
    // private Boolean twoFactorEnabled = false;
    // private String twoFactorSecret;  // For TOTP (Google Authenticator)
    // private String twoFactorBackupCodes;  // JSON array of backup codes

    private String role = "ROLE_USER";
    private Integer roleVersion = 0;

    private Instant createdAt;
    private Instant updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OAuthProvider> oAuthProviders = new ArrayList<>();

    @PrePersist
    protected void onCreate(){
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if(this.username != null) this.username = this.username.toLowerCase();
        if(this.role == null) this.role = "ROLE_USER";
        if(this.roleVersion == null) this.roleVersion = 0;
    }

    @PreUpdate
    protected void onUpdate(){
        this.updatedAt = Instant.now();
        if(this.username != null) this.username = this.username.toLowerCase();
    }

    public void setUsername(String username){
        this.username = username != null? username.toLowerCase() : null;
    }

    public void setImageProperties(Map<String, Object> imageProperties){
        this.setImagePublicId((String) imageProperties.get("public_id"));
        this.setImageUrl((String) imageProperties.get("secure_url"));
    }

    public UserDto toDto() {
        List<String> providers = this.oAuthProviders.stream()
                .map(OAuthProvider::getProvider)
                .toList();

        return new UserDto(
                this.userId,
                this.username != null ? this.username.toLowerCase() : null,
                this.name,
                this.email,
                this.secondaryEmail,
                this.emailVerified,
                this.createdAt,
                this.updatedAt,
                this.imagePublicId,
                this.imageUrl,
                providers,
                this.password != null && !this.password.isEmpty()

        );
    }

//    public boolean getEmailVerified() {
//        return this.emailVerified;
//    }
}


