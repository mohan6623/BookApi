package com.marvel.springsecurity.service;

import com.marvel.springsecurity.model.BookModel;
import com.marvel.springsecurity.repo.BookRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    private BookRepo repo;

    public BookService(BookRepo repo) {
        this.repo = repo;    }

    public String addBook(BookModel book){
        repo.save(book);
        return "Book added successfully";
    }


    public List<BookModel> getBooks() {
        return repo.findAll();
    }

    public void updateBook(BookModel book) {
        repo.save(book);
    }


    public void deleteBook(int id) {
        repo.deleteById(id);
    }

    public BookModel getBookById(int id) {
        return repo.findById(id).orElse(null);
    }

    public List<BookModel> getBookByTitle(String title) {
        return repo.findByBookTitle(title);
    }
}
