package com.scaler.harsh.cartservice.repository;

import com.scaler.harsh.cartservice.model.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartRepository extends MongoRepository<Cart, Integer> {
    List<Cart> findByUserId(Integer userId);
}