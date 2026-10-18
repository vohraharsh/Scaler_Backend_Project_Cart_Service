package com.scaler.harsh.cartservice.dto;

import com.scaler.harsh.cartservice.model.Cart;
import com.scaler.harsh.cartservice.model.CartItem;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class FakeStoreCartDTO {
    private int id;
    private int userId;
    private String date;
    private List<CartItem> products;

    public Cart toCart() {
        Cart cart = new Cart();
        cart.setId(this.id);
        cart.setUserId(this.userId);
        cart.setDate(this.date);
        cart.setProducts(this.products);
        return cart;
    }

    public static FakeStoreCartDTO from(CreateCartRequestDTO request) {
        FakeStoreCartDTO dto = new FakeStoreCartDTO();
        dto.setUserId(request.getUserId());
        dto.setDate(request.getDate());
        dto.setProducts(request.getProducts());
        return dto;
    }
}