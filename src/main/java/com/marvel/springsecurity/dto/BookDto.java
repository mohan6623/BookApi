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
    private String imageUrl;
    private String imagePublicId;
    private double averageRating;
    private long noOfRatings;

    public BookDto(Book book) {
        this.id = book.getBookId();
        this.title = book.getTitle();
        this.description = book.getDescription();
        this.author = book.getAuthor();
        this.category = book.getCategory();
        this.imageUrl = book.getImageUrl();
        this.imagePublicId = book.getImagePublicId();
    }

    public BookDto(Book book, AvgAndCountProjection obj){
        this(book);
        this.averageRating = obj.getAverage();
        this.noOfRatings =obj.getCount();
    }

    public BookDto(Object[] line){
        this((Book)line[0]);
        this.averageRating = line[1] != null ? (Double) line[1] : 0.0;
        this.noOfRatings = line[2] != null ? (Long) line[2] : 0;
    }

    public BookDto(Book book, Double avg){
        this(book);
        this.averageRating = avg != null ? avg : 0.0;
    }
}
