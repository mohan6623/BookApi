package com.marvel.springsecurity.repo;

import com.marvel.springsecurity.dto.projections.AuthorAndCountProjection;
import com.marvel.springsecurity.dto.projections.CategoryAndCountProjection;
import com.marvel.springsecurity.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepo extends JpaRepository<Book, Integer> {
// PostgreSQL specific query
//    @Query(value = "SELECT * FROM book_model b WHERE b.title ~* ?1", nativeQuery = true) // for case-insensitive search "~*".
// JPQL query
//    @Query("SELECT b FROM BookModel b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%',?1,'%'))")  // for case-insensitive search use "LOWER()".
//    @Query(value = "SELECT * FROM book_model b WHERE b.title ILIKE CONCAT('%', ?1, '%')", nativeQuery = true)
//    List<Book> findByBookTitle(String title);



    @Query("""
        SELECT b, COALESCE(AVG(r.rating), 0), COUNT(r.id)
        FROM Book b
        LEFT JOIN Rating r On r.book.bookId = b.bookId
        WHERE (:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%')))
          OR (:author IS NULL OR LOWER(b.author) LIKE LOWER(CONCAT('%', :author, '%')))
          OR (:category IS NULL OR LOWER(b.category) LIKE LOWER(CONCAT('%', :category, '%')))
        GROUP BY b
        """)
    Page<Object[]> searchBooks(@Param("title") String title,
                               @Param("author") String author,
                               @Param("category") String category,
                               Pageable pageable);
    @Query("""
            SELECT b, COALESCE(AVG(r.rating), 0), COUNT(r.id)
            FROM Book b
            LEFT JOIN Rating r On r.book.bookId = b.bookId
            GROUP BY b.bookId, b.author, b.category, b.description, b.imagePublicId, b.imageUrl, b.title
            """)
    Page<Object[]> findBooksWithRatings(Pageable pageable);



   @Query("""
            SELECT b.category AS category, COUNT(*) AS counts FROM Book b
            GROUP BY b.category
            ORDER BY counts
          """)
   List<CategoryAndCountProjection> getDistinctCategoriesAndCount();

   @Query("""
            SELECT b.author AS author, COUNT(*) AS counts FROM Book b
            GROUP BY b.author
            ORDER BY counts ASC
          """)
   List<AuthorAndCountProjection> getDistinctAuthorsAndCount();
}
