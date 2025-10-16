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
    private long noOfRatings;

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

    public BookDto(Book book, Number[] obj){
        this(book);
        this.averageRating =obj[0] != null ? (obj[0]).doubleValue() : 0.0;
        this.noOfRatings = obj[1] != null ? (obj[1]).longValue() : 0;
    }

    public BookDto(Object[] line){
        this((Book)line[0]);
        this.averageRating = line[1] != null ? ((Number) line[1]).doubleValue() : 0.0;
        this.noOfRatings = line[2] != null ? ((Number) line[2]).longValue() : 0;
    }

    public BookDto(Book book, Number avg){
        this(book);
        this.averageRating = avg != null ? avg.doubleValue() : 0.0;
    }
}
