package com.marvel.springsecurity.dto;

import com.marvel.springsecurity.model.Book;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDto {
    private int id;
    private String title;
    private String description;
    private String author;
    private String category;
    private String imageName;
    private String imageType;
    private String imageBase64;
    private double averageRating;
    private int noOfRatings;

    public BookDto(Book book) {
        this.id = book.getId();
        this.title = book.getTitle();
        this.description = book.getDescription();
        this.author = book.getAuthor();
        this.category = book.getCategory();
        this.imageName = book.getImageName();
        this.imageType = book.getImageType();
        this.imageBase64 = book.getImageBase64();
    }


    public BookDto(Book book, Object[] obj){
        this(book);
        this.setAverageRating((Double) obj[0]);
        this.setNoOfRatings((Integer) obj[1]);
    }
}
