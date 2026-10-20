package com.scaler.harsh.cartservice.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class CartEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public CartEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishItemAddedToCart(Integer cartId, Integer userId, Integer productId, Integer quantity) {
        String message = "Cart " + cartId + " (user " + userId + "): added productId " + productId
                + " x" + quantity;
        kafkaTemplate.send("cart-item-added", message);
    }
}