package com.marvel.springsecurity.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
@Entity
@Data
@AllArgsConstructor
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

    private transient String imageBase64;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int roleVersion = 0;

    public String getImageBase64(){
        return profilePic != null ? Base64.getEncoder().encodeToString(profilePic) : null;
    }

//    public User(User user){
//        this.setId(user.getId());
//        this.getUsername(user.getUsername())
//    }
}