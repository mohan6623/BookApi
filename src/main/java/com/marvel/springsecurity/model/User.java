package com.marvel.springsecurity.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;


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
    private String imageUrl;
    private String imagePublicId;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int roleVersion = 0;

    public User(int id, String username, String password, String role, String mail, String imagePublicId,String imageUrl, int roleVersion) {
        this.id = id;
        this.username = username.toLowerCase();
        this.password = password;
        this.role = role;
        this.mail = mail.toLowerCase();
        this.imagePublicId = imagePublicId;
        this.imageUrl = imageUrl;
        this.roleVersion = roleVersion;
    }

    public void setUsername(String username){
        this.username = username.toLowerCase();
    }
    public void setMail(String mail){
        this.mail = mail.toLowerCase();
    }
}