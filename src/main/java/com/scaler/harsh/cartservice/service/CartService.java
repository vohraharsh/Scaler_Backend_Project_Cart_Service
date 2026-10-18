package com.scaler.harsh.cartservice.service;

import com.scaler.harsh.cartservice.dto.CreateCartRequestDTO;
import com.scaler.harsh.cartservice.model.Cart;

import java.util.List;

public interface CartService {
    Cart getCartById(Integer id);
    List<Cart> getAllCarts();
    List<Cart> getCartsByUserId(Integer userId);
    Cart createCart(CreateCartRequestDTO request);
    Cart updateCart(Integer id, CreateCartRequestDTO request);
    void deleteCart(Integer id);
}