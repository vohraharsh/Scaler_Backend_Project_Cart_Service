package com.scaler.harsh.cartservice.model;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Embeddable
public class CartItem implements Serializable {
    private Integer productId;
    private Integer quantity;
}