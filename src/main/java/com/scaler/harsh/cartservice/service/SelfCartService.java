package com.scaler.harsh.cartservice.service;

import com.scaler.harsh.cartservice.dto.CreateCartRequestDTO;
import com.scaler.harsh.cartservice.exceptions.ServiceException;
import com.scaler.harsh.cartservice.model.Cart;
import com.scaler.harsh.cartservice.repository.CartRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Primary
public class SelfCartService implements CartService {

    private final CartRepository cartRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    public SelfCartService(CartRepository cartRepository, SequenceGeneratorService sequenceGeneratorService) {
        this.cartRepository = cartRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    @Override
    @Cacheable(value = "carts", key = "#id")
    public Cart getCartById(Integer id) {
        Optional<Cart> cart = cartRepository.findById(id);
        if (cart.isEmpty()) {
            throw new ServiceException("Cart not found for id: " + id, HttpStatus.NOT_FOUND);
        }
        return cart.get();
    }

    @Override
    public List<Cart> getAllCarts() {
        return cartRepository.findAll();
    }

    @Override
    public List<Cart> getCartsByUserId(Integer userId) {
        List<Cart> carts = cartRepository.findByUserId(userId);
        if (carts.isEmpty()) {
            throw new ServiceException("Carts not found for user: " + userId, HttpStatus.NOT_FOUND);
        }
        return carts;
    }

    @Override
    public Cart createCart(CreateCartRequestDTO request) {
        Cart cart = new Cart();
        cart.setId(sequenceGeneratorService.generateSequence("carts_sequence"));
        cart.setUserId(request.getUserId());
        cart.setDate(LocalDate.now().toString());
        cart.setProducts(request.getProducts());
        return cartRepository.save(cart);
    }

    @Override
    @CachePut(value = "carts", key = "#id")
    public Cart updateCart(Integer id, CreateCartRequestDTO request) {
        Optional<Cart> existing = cartRepository.findById(id);
        if (existing.isEmpty()) {
            throw new ServiceException("Cart not found for id: " + id, HttpStatus.NOT_FOUND);
        }

        Cart cart = existing.get();
        cart.setUserId(request.getUserId());
        cart.setProducts(request.getProducts());

        return cartRepository.save(cart);
    }

    @Override
    @CacheEvict(value = "carts", key = "#id")
    public void deleteCart(Integer id) {
        if (!cartRepository.existsById(id)) {
            throw new ServiceException("Cart not found for id: " + id, HttpStatus.NOT_FOUND);
        }
        cartRepository.deleteById(id);
    }
}