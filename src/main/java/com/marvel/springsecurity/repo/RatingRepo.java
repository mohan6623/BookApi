package com.marvel.springsecurity.repo;

import com.marvel.springsecurity.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RatingRepo extends JpaRepository<Rating, Integer> {

    List<Rating> findAllByBookId(int bookId);

    void deleteAllByBookId(int id);


    @Query("SELECT AVG(r.rating), COUNT(r.rating) FROM Rating r WHERE r.book.id = :bookId")
    Object[] AverageAndCountByBookId(int bookId);
}
