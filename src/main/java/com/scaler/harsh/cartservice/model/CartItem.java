package com.scaler.harsh.cartservice.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CartItem {
    private Integer productId;
    private Integer quantity;
}