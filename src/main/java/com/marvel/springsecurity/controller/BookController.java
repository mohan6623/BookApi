package com.marvel.springsecurity.controller;


import com.marvel.springsecurity.model.BookModel;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class BookController {


    List<BookModel> books = new ArrayList<>(List.of(new BookModel(1,"Atomic Habit"), new BookModel(2,"Ikigai")));
    @GetMapping("books")
    public List<BookModel> home(HttpServletRequest request){
        return books;
    }

    @GetMapping("csrf-token")
    public CsrfToken getCsrf(HttpServletRequest request){
        return (CsrfToken) request.getAttribute("_csrf");
    }

    @PostMapping("add")
    public String addBook(@RequestBody BookModel model){
        books.add(model);
        return "Added Successfully";
    }

}
