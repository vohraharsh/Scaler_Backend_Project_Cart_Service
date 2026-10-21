package com.scaler.harsh.cartservice.controller;

import com.scaler.harsh.cartservice.dto.AddItemToCartRequestDTO;
import com.scaler.harsh.cartservice.dto.CartResponseDTO;
import com.scaler.harsh.cartservice.dto.CreateCartRequestDTO;
import com.scaler.harsh.cartservice.exceptions.ServiceException;
import com.scaler.harsh.cartservice.model.Cart;
import com.scaler.harsh.cartservice.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.scaler.harsh.cartservice.model.CartItem;

import java.util.ArrayList;
import java.util.List;

@RestController
public class CartController {

    private final CartService svc;

    public CartController(CartService svc) {
        this.svc = svc;
    }

    @GetMapping("/carts")
    public List<CartResponseDTO> getAllCarts() {
        List<Cart> carts = svc.getAllCarts();
        List<CartResponseDTO> response = new ArrayList<>();
        for (Cart cart : carts) {
            response.add(convertToResponseDTO(cart));
        }
        return response;
    }

    @GetMapping("/carts/{id}")
    public CartResponseDTO getCartById(@PathVariable("id") Integer id) {
        if (id <= 0) {
            throw new ServiceException("Invalid cart id", HttpStatus.BAD_REQUEST);
        }
        return convertToResponseDTO(svc.getCartById(id));
    }

    @GetMapping("/carts/user/{userId}")
    public List<CartResponseDTO> getCartsByUserId(@PathVariable("userId") Integer userId) {
        if (userId <= 0) {
            throw new ServiceException("Invalid user id", HttpStatus.BAD_REQUEST);
        }
        List<Cart> carts = svc.getCartsByUserId(userId);
        List<CartResponseDTO> response = new ArrayList<>();
        for (Cart cart : carts) {
            response.add(convertToResponseDTO(cart));
        }
        return response;
    }

    @PostMapping("/carts")
    public ResponseEntity<CartResponseDTO> createCart(@RequestBody CreateCartRequestDTO request) {
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new ServiceException("Valid userId is required", HttpStatus.BAD_REQUEST);
        }
        if (request.getProducts() == null || request.getProducts().isEmpty()) {
            throw new ServiceException("Cart must contain at least one product", HttpStatus.BAD_REQUEST);
        }
        Cart cart = svc.createCart(request);
        return new ResponseEntity<>(convertToResponseDTO(cart), HttpStatus.CREATED);
    }

    @PutMapping("/carts/{id}")
    public CartResponseDTO updateCart(@PathVariable("id") Integer id,
                                      @RequestBody CreateCartRequestDTO request) {
        if (id <= 0) {
            throw new ServiceException("Invalid cart id", HttpStatus.BAD_REQUEST);
        }
        return convertToResponseDTO(svc.updateCart(id, request));
    }

    @DeleteMapping("/carts/{id}")
    public String deleteCart(@PathVariable("id") Integer id) {
        if (id <= 0) {
            throw new ServiceException("Invalid cart id", HttpStatus.BAD_REQUEST);
        }
        svc.deleteCart(id);
        return "Cart with id " + id + " deleted";
    }

    @PostMapping("/carts/items")
    public CartResponseDTO addItemToCart(@RequestBody AddItemToCartRequestDTO request) {
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new ServiceException("Valid userId is required", HttpStatus.BAD_REQUEST);
        }
        if (request.getProductId() == null || request.getProductId() <= 0) {
            throw new ServiceException("Valid productId is required", HttpStatus.BAD_REQUEST);
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new ServiceException("Quantity must be a positive number", HttpStatus.BAD_REQUEST);
        }
        Cart cart = svc.addItemToCart(request.getUserId(), request.getProductId(), request.getQuantity());
        return convertToResponseDTO(cart);
    }

    private CartResponseDTO convertToResponseDTO(Cart cart) {
        CartResponseDTO dto = new CartResponseDTO();
        dto.setId(cart.getId());
        dto.setUserId(cart.getUserId());
        dto.setDate(cart.getDate());
        dto.setProducts(cart.getProducts());
        dto.setTotal(computeTotal(cart.getProducts()));
        return dto;
    }

    private double computeTotal(List<CartItem> items) {
        if (items == null || items.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (CartItem item : items) {
            total += item.getPrice() * item.getQuantity();
        }
        return total;
    }

    @GetMapping("/carts/active/{userId}")
    public CartResponseDTO getActiveCartByUserId(@PathVariable("userId") Integer userId) {
        if (userId <= 0) {
            throw new ServiceException("Invalid user id", HttpStatus.BAD_REQUEST);
        }
        return convertToResponseDTO(svc.getActiveCartByUserId(userId));
    }
}