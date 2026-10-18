package com.scaler.harsh.cartservice.service;

import com.scaler.harsh.cartservice.dto.CreateCartRequestDTO;
import com.scaler.harsh.cartservice.dto.FakeStoreCartDTO;
import com.scaler.harsh.cartservice.exceptions.ServiceException;
import com.scaler.harsh.cartservice.model.Cart;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class FakeStoreCartService implements CartService {

    private final RestTemplate restTemplate;

    public FakeStoreCartService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Cart getCartById(Integer id) {
        ResponseEntity<FakeStoreCartDTO> response;
        try {
            response = restTemplate.getForEntity(
                    "https://fakestoreapi.com/carts/" + id, FakeStoreCartDTO.class);
        } catch (RestClientException e) {
            throw new ServiceException("Could not reach FakeStoreAPI", HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (response.getBody() == null) {
            throw new ServiceException("Cart not found for id: " + id, HttpStatus.NOT_FOUND);
        }

        return response.getBody().toCart();
    }

    @Override
    public List<Cart> getAllCarts() {
        ResponseEntity<FakeStoreCartDTO[]> response;
        try {
            response = restTemplate.getForEntity(
                    "https://fakestoreapi.com/carts", FakeStoreCartDTO[].class);
        } catch (RestClientException e) {
            throw new ServiceException("Could not reach FakeStoreAPI", HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (response.getBody() == null) {
            throw new ServiceException("Cart list not found", HttpStatus.NOT_FOUND);
        }

        List<Cart> carts = new ArrayList<>();
        for (FakeStoreCartDTO dto : response.getBody()) {
            carts.add(dto.toCart());
        }
        return carts;
    }

    @Override
    public List<Cart> getCartsByUserId(Integer userId) {
        ResponseEntity<FakeStoreCartDTO[]> response;
        try {
            response = restTemplate.getForEntity(
                    "https://fakestoreapi.com/carts/user/" + userId, FakeStoreCartDTO[].class);
        } catch (RestClientException e) {
            throw new ServiceException("Could not reach FakeStoreAPI", HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (response.getBody() == null) {
            throw new ServiceException("Carts not found for user: " + userId, HttpStatus.NOT_FOUND);
        }

        List<Cart> carts = new ArrayList<>();
        for (FakeStoreCartDTO dto : response.getBody()) {
            carts.add(dto.toCart());
        }
        return carts;
    }

    @Override
    public Cart createCart(CreateCartRequestDTO request) {
        FakeStoreCartDTO requestDto = FakeStoreCartDTO.from(request);
        FakeStoreCartDTO responseDto;

        try {
            responseDto = restTemplate.postForObject(
                    "https://fakestoreapi.com/carts", requestDto, FakeStoreCartDTO.class);
        } catch (RestClientException e) {
            throw new ServiceException("Could not reach FakeStoreAPI", HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (responseDto == null) {
            throw new ServiceException("Cart creation failed", HttpStatus.BAD_REQUEST);
        }

        Cart cart = requestDto.toCart();
        cart.setId(responseDto.getId());
        return cart;
    }

    @Override
    public Cart updateCart(Integer id, CreateCartRequestDTO request) {
        FakeStoreCartDTO requestDto = FakeStoreCartDTO.from(request);
        HttpEntity<FakeStoreCartDTO> requestEntity = new HttpEntity<>(requestDto);

        ResponseEntity<FakeStoreCartDTO> response;
        try {
            response = restTemplate.exchange(
                    "https://fakestoreapi.com/carts/" + id,
                    HttpMethod.PUT, requestEntity, FakeStoreCartDTO.class);
        } catch (RestClientException e) {
            throw new ServiceException("Could not reach FakeStoreAPI", HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (response.getBody() == null) {
            throw new ServiceException("Cart not found for id: " + id, HttpStatus.NOT_FOUND);
        }

        Cart cart = requestDto.toCart();
        cart.setId(id);
        return cart;
    }

    @Override
    public void deleteCart(Integer id) {
        try {
            restTemplate.delete("https://fakestoreapi.com/carts/" + id);
        } catch (RestClientException e) {
            throw new ServiceException("Could not reach FakeStoreAPI", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}