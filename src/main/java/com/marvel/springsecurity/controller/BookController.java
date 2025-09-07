package com.marvel.springsecurity.controller;

import com.marvel.springsecurity.model.BookModel;
import com.marvel.springsecurity.service.BookService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BookController {

    private final BookService service;

    public BookController(BookService service) {
        this.service = service;
    }

    @PostMapping(path = "/addbook", consumes = {"application/xml"})
    public String addBook(@RequestBody BookModel book){
        service.addBook(book);
        return "Book added successfully";
    }

    @GetMapping(path = "/books", produces = {"application/json"})
    public List<BookModel> getBooks(){
        return service.getBooks();
    }

    @GetMapping("/book/{id}")
    public BookModel getBook(@PathVariable int id){
        return service.getBookById(id);
    }

    @PutMapping("/book")
    public String updateBook(@RequestBody BookModel book){
        service.updateBook(book);
        return "Book updated successfully";
    }

    @DeleteMapping("/book/{id}")
    public String deleteBook(@PathVariable int id) {
        service.deleteBook(id);
        return "Book deleted successfully";
    }

    @GetMapping("/title/{title}")
    public List<BookModel> getBookByTitle(@PathVariable String title){
        return service.getBookByTitle(title);
    }
}
