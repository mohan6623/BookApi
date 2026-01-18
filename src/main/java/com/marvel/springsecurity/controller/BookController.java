package com.marvel.springsecurity.controller;

import com.marvel.springsecurity.dto.BookDto;
import com.marvel.springsecurity.dto.CommentsDto;
import com.marvel.springsecurity.dto.projections.AuthorAndCountProjection;
import com.marvel.springsecurity.dto.projections.CategoryAndCountProjection;
import com.marvel.springsecurity.model.Book;
import com.marvel.springsecurity.model.Comment;
import com.marvel.springsecurity.model.Rating;
import com.marvel.springsecurity.service.book.BookService;
import com.marvel.springsecurity.service.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api")
public class BookController {

    private final BookService bookService;

    // for constructor injection
    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping(path = "/addbook")
    public ResponseEntity<String> addBook(@RequestPart Book book, @RequestPart MultipartFile imageFile) {
        try {
            // System.out.println("book = " + book);
            bookService.addBook(book, imageFile);
            return ResponseEntity.status(HttpStatus.CREATED).body("Book added successfully");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to add book: " + e.getMessage());
        }
    }

    // @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @GetMapping("/books") // , produces = {"application/json"})
    public ResponseEntity<Page<BookDto>> getBooks(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<BookDto> books = bookService.getBooks(page, size);
        // if (!books.isEmpty()) {
        // System.out.println("📚 [Backend /api/books] Returning " +
        // books.getContent().size() + " books:");
        // books.getContent().forEach(book ->
        // System.out.println(" - ID: " + book.getId() + " | Title: \"" +
        // book.getTitle() + "\"")
        // );
        // }
        if (books.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(books);
    }

    // @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @GetMapping("/bookid/{id}")
    public ResponseEntity<BookDto> getBook(@PathVariable int id) {
        // System.out.println("📖 [Backend /api/bookid/" + id + "] Fetching book with
        // ID: " + id);
        BookDto book = bookService.getBookById(id);
        if (book != null) {
            // System.out.println(" ✅ Found: \"" + book.getTitle() + "\" (ID: " +
            // book.getId() + ")");
            return ResponseEntity.ok(book);
        }
        log.debug("Book not found with id: {}", id);
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/book/{id}")
    public ResponseEntity<String> updateBook(@PathVariable int id, @RequestPart Book book,
            @RequestPart(required = false) MultipartFile imageFile) {
        try {
            boolean updated = bookService.updateBook(id, book, imageFile);
            if (updated) {
                return ResponseEntity.ok("Book updated successfully");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Book not found");
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update book: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/book/{id}")
    public ResponseEntity<String> deleteBook(@PathVariable int id) {
        try {
            bookService.deleteBook(id);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.ok("Book deleted successfully");
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @PostMapping("/book/{id}/rating")
    public ResponseEntity<Void> addRating(@PathVariable int id,
            @RequestBody Rating review,
            @AuthenticationPrincipal UserPrincipal user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        bookService.addRating(id, review, user.getUserId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/book/{id}/ratings")
    public ResponseEntity<Map<Integer, Integer>> getRatings(@PathVariable int id) {
        Map<Integer, Integer> ratings = bookService.getRatings(id);
        return ResponseEntity.ok(ratings);
    }

    // @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @GetMapping("/book/{id}/comment")
    public ResponseEntity<Page<CommentsDto>> getComments(@PathVariable int id, @RequestParam int page,
            @RequestParam int size) {
        Page<CommentsDto> comments = bookService.getComments(id, page, size);
        if (comments.isEmpty())
            return ResponseEntity.noContent().build();
        return ResponseEntity.ok(comments);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @PostMapping("/book/{bookId}/comment")
    public ResponseEntity<CommentsDto> addComment(@PathVariable int bookId,
            @RequestBody CommentsDto commentDto,
            @AuthenticationPrincipal UserPrincipal principal) {
        Comment comment = new Comment(commentDto, principal.getUserId());
        var newComment = bookService.addComment(bookId, comment, principal.getUserId());
        return ResponseEntity.ok(newComment);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @PutMapping("/book/{bookId}/comment")
    public ResponseEntity<CommentsDto> updateComment(@RequestBody CommentsDto comment,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (comment == null)
            return ResponseEntity.status(406).build();
        var updated = bookService.updateComment(comment, principal.getUserId());
        return ResponseEntity.ok(new CommentsDto(updated));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @DeleteMapping("/comment/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable int commentId) {
        bookService.deleteComment(commentId);
        return ResponseEntity.ok().build();
    }

    // @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @GetMapping("/books/search")
    public ResponseEntity<Page<BookDto>> searchBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Page<BookDto> books = bookService.searchBooks(title, author, category, page, size);
        if (books == null)
            return ResponseEntity.noContent().build();
        return ResponseEntity.ok(books);
    }

    /**
     * Returns List of book category along with their count
     **/
    @GetMapping("/book/categories")
    public ResponseEntity<List<CategoryAndCountProjection>> getCategories() {
        List<CategoryAndCountProjection> categories = bookService.getDistinctCategoriesAndCount();
        if (categories == null)
            return ResponseEntity.noContent().build();
        return ResponseEntity.ok(categories);
    }

    /**
     * Returns List Authors along with their count
     **/
    @GetMapping("/book/authors")
    public ResponseEntity<List<AuthorAndCountProjection>> getAuthors() {
        List<AuthorAndCountProjection> authors = bookService.getDistinctAuthorsAndCount();
        if (authors == null)
            return ResponseEntity.noContent().build();
        return ResponseEntity.ok(authors);
    }
}
