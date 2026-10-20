package com.scaler.harsh.cartservice.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Document(collection = "carts")
public class Cart implements Serializable {

    @Id
    private Integer id;

    private Integer userId;
    private String date;

    private List<CartItem> products;
}