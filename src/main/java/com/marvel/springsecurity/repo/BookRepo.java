package com.marvel.springsecurity.repo;

import com.marvel.springsecurity.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepo extends JpaRepository<Book, Integer> {
// PostgreSQL specific query
//    @Query(value = "SELECT * FROM book_model b WHERE b.title ~* ?1", nativeQuery = true) // for case-insensitive search "~*".
// JPQL query
//    @Query("SELECT b FROM BookModel b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%',?1,'%'))")  // for case-insensitive search use "LOWER()".
//    @Query(value = "SELECT * FROM book_model b WHERE b.title ILIKE CONCAT('%', ?1, '%')", nativeQuery = true)
//    List<Book> findByBookTitle(String title);

    @Query("SELECT b, AVG(r.rating) " +
            "FROM Book b " +
            "LEFT JOIN Rating r ON r.book.id = b.id " +
            "WHERE (:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
            "AND (:author IS NULL OR LOWER(b.author) LIKE LOWER(CONCAT('%', :author, '%'))) " +
            "AND (:category IS NULL OR LOWER(b.category) LIKE LOWER(CONCAT('%', :category, '%'))) " +
            "GROUP BY b")
    Page<Object[]> searchBooks(String title, String author, String category, Pageable pageable);

    @Query("""
            SELECT b, COALESCE(AVG(r.rating), 0), COUNT(r.id)
            FROM Book b
            LEFT JOIN Rating r On r.book.id = b.id
            GROUP BY b
            """)
    Page<Object[]> findBooksWithRatings(Pageable pageable);


    
}
