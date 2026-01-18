package com.marvel.springsecurity.repo;

import com.marvel.springsecurity.dto.projections.AvgAndCountProjection;
import com.marvel.springsecurity.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RatingRepo extends JpaRepository<Rating, Integer> {

    List<Rating> findAllByBook_BookId(int bookId);

    void deleteAllByBook_BookId(int id);


    @Query("SELECT COALESCE(AVG(r.rating), 0) AS average, COALESCE(COUNT(r.rating), 0) As count FROM Rating r WHERE r.book.bookId = :bookId")
    AvgAndCountProjection AverageAndCountByBookId(@Param("bookId") int bookId);

    //add findByUser_UserIdAndBook_BookId
    Rating findByUser_UserIdAndBook_BookId(int userId, int bookId);
}
