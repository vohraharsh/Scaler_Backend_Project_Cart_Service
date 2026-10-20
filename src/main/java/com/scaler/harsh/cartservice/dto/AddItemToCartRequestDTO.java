package com.scaler.harsh.cartservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddItemToCartRequestDTO {
    private Integer userId;
    private Integer productId;
    private Integer quantity;
}