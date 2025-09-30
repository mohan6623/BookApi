package com.marvel.springsecurity.repo;

import com.marvel.springsecurity.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepo extends JpaRepository<Rating, Integer> {

}
