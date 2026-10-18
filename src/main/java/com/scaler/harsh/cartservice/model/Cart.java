package com.scaler.harsh.cartservice.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter @Setter
public class Cart implements Serializable {
    private Integer id;
    private Integer userId;
    private String date;
    private List<CartItem> products;
}