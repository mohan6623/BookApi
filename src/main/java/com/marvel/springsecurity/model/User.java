package com.marvel.springsecurity.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Base64;


@Entity
@Data
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String username;
    private String password;
    private String role;
    private String mail;
    private String imageName;
    private String imageType;
    private byte[] profilePic;

    @Transient
    private transient String imageBase64;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int roleVersion = 0;

    public User(int id, String username, String password, String role, String mail, String imageName, String imageType, byte[] profilePic, String imageBase64, int roleVersion) {
        this.id = id;
        this.username = username.toLowerCase();
        this.password = password;
        this.role = role;
        this.mail = mail.toLowerCase();
        this.imageName = imageName;
        this.imageType = imageType;
        this.profilePic = profilePic;
        this.imageBase64 = imageBase64;
        this.roleVersion = roleVersion;
    }

    public void setUsername(String username){
        this.username = username.toLowerCase();
    }
    public void setMail(String mail){
        this.mail = mail.toLowerCase();
    }
    public String getImageBase64(){
        return profilePic != null ? Base64.getEncoder().encodeToString(profilePic) : null;
    }
}