package com.scaler.harsh.cartservice.dto;

import com.scaler.harsh.cartservice.model.CartItem;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class CreateCartRequestDTO {
    private Integer userId;
    private String date;
    private List<CartItem> products;
}