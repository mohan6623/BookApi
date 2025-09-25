package com.marvel.springsecurity.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookModel {

    @Id
    private int id;
    private String title;
    private String description;
    private String author;
    private String category;
    private String imageName;
    private String imageType;
    private byte[] image;



}
