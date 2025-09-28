package com.marvel.springsecurity.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String title;
    private String description;
    private String author;
    private String category;
    private String imageName;
    private String imageType;
    private byte[] image;

    private transient String imageBase64;

    public String getImageBase64() {
//        System.out.println("imageBase64 : "+ java.util.Base64.getEncoder().encodeToString(image));
        return image != null ? java.util.Base64.getEncoder().encodeToString(image) : null;
    }

}