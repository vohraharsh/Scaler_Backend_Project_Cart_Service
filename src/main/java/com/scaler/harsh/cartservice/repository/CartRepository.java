package com.scaler.harsh.cartservice.repository;

import com.scaler.harsh.cartservice.model.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends MongoRepository<Cart, Integer> {
    List<Cart> findByUserId(Integer userId);
    Optional<Cart> findFirstByUserId(Integer userId);
}