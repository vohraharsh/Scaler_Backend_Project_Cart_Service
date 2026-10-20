package com.scaler.harsh.cartservice.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class CartItem implements Serializable {
    private Integer productId;
    private Integer quantity;
}