package com.scaler.harsh.cartservice.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class Cart {
    private Integer id;
    private Integer userId;
    private String date;
    private List<CartItem> products;
}