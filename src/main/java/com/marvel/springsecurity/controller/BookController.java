package com.marvel.springsecurity.controller;

import com.marvel.springsecurity.model.BookModel;
import com.marvel.springsecurity.service.BookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
public class BookController {

    private final BookService service;
//for constructor injection
    public BookController(BookService service) {
        this.service = service;
    }
    @PostMapping(path = "/addbook")
    public ResponseEntity<String> addBook(@RequestPart BookModel book, @RequestPart MultipartFile imageFile) {
        try {
        service.addBook(book, imageFile);
        return ResponseEntity.status(HttpStatus.CREATED).body("Book added successfully");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add book: " + e.getMessage());
        }
    }

    @GetMapping(path = "/books")  //, produces = {"application/json"})
    public ResponseEntity<List<BookModel>> getBooks(){
        List<BookModel> books = service.getBooks();
        if (books.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(books);
    }

    @GetMapping("/book/{id}")
    public ResponseEntity<BookModel> getBook(@PathVariable int id){
        BookModel book = service.getBookById(id);
        if (book == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(book);
    }

    @PutMapping("/book")
    public ResponseEntity<String> updateBook(@PathVariable int id, @RequestPart BookModel book, @RequestPart(required = false) MultipartFile imageFile) {
        try {
            Boolean updated = service.updateBook(id, book, imageFile);
            return ResponseEntity.ok("Book updated successfully");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update book: " + e.getMessage());
        }
    }

    @DeleteMapping("/book/{id}")
    public ResponseEntity<String> deleteBook(@PathVariable int id) {
        service.deleteBook(id);
        return ResponseEntity.ok("Book deleted successfully");
    }

    @GetMapping("/title/{title}")
    public ResponseEntity<List<BookModel>> getBookByTitle(@PathVariable String title){
        List<BookModel> books = service.getBookByTitle(title);
        if(books.isEmpty()){
            return ResponseEntity.noContent().build();
//            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return ResponseEntity.ok(books);
    }
}
