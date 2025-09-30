package com.marvel.springsecurity.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private int rating;
//    private String review;
//    private String userName;
//    private int contentQuality;
//    private int valueForMoney;
//    private int easeToUnderstand;
//    private int Recommendation;
    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;

}
