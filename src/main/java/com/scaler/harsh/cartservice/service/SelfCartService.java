package com.scaler.harsh.cartservice.service;

import com.scaler.harsh.cartservice.dto.CreateCartRequestDTO;
import com.scaler.harsh.cartservice.dto.ProductDTO;
import com.scaler.harsh.cartservice.exceptions.ServiceException;
import com.scaler.harsh.cartservice.kafka.CartEventProducer;
import com.scaler.harsh.cartservice.model.Cart;
import com.scaler.harsh.cartservice.model.CartItem;
import com.scaler.harsh.cartservice.repository.CartRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Primary
public class SelfCartService implements CartService {

    private static final String PRODUCT_SERVICE_URL = "http://localhost:8080";

    private final CartRepository cartRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final RestTemplate restTemplate;
    private final CartEventProducer cartEventProducer;

    public SelfCartService(CartRepository cartRepository,
                           SequenceGeneratorService sequenceGeneratorService,
                           RestTemplate restTemplate,
                           CartEventProducer cartEventProducer) {
        this.cartRepository = cartRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.restTemplate = restTemplate;
        this.cartEventProducer = cartEventProducer;
    }

    @Override
    @Cacheable(value = "carts", key = "#id")
    public Cart getCartById(Integer id) {
        Optional<Cart> cart = cartRepository.findById(id);
        if (cart.isEmpty()) {
            throw new ServiceException("Cart not found for id: " + id, HttpStatus.NOT_FOUND);
        }
        return cart.get();
    }

    @Override
    public List<Cart> getAllCarts() {
        return cartRepository.findAll();
    }

    @Override
    public List<Cart> getCartsByUserId(Integer userId) {
        List<Cart> carts = cartRepository.findByUserId(userId);
        if (carts.isEmpty()) {
            throw new ServiceException("Carts not found for user: " + userId, HttpStatus.NOT_FOUND);
        }
        return carts;
    }

    @Override
    public Cart createCart(CreateCartRequestDTO request) {
        Cart cart = new Cart();
        cart.setId(sequenceGeneratorService.generateSequence("carts_sequence"));
        cart.setUserId(request.getUserId());
        cart.setDate(LocalDate.now().toString());
        cart.setProducts(request.getProducts());
        return cartRepository.save(cart);
    }

    @Override
    @CachePut(value = "carts", key = "#id")
    public Cart updateCart(Integer id, CreateCartRequestDTO request) {
        Optional<Cart> existing = cartRepository.findById(id);
        if (existing.isEmpty()) {
            throw new ServiceException("Cart not found for id: " + id, HttpStatus.NOT_FOUND);
        }

        Cart cart = existing.get();
        cart.setUserId(request.getUserId());
        cart.setProducts(request.getProducts());

        return cartRepository.save(cart);
    }

    @Override
    @CacheEvict(value = "carts", key = "#id")
    public void deleteCart(Integer id) {
        if (!cartRepository.existsById(id)) {
            throw new ServiceException("Cart not found for id: " + id, HttpStatus.NOT_FOUND);
        }
        cartRepository.deleteById(id);
    }

    @Override
    @CachePut(value = "carts", key = "#result.id")
    public Cart addItemToCart(Integer userId, Integer productId, Integer quantity) {

        // Validate the product actually exists before it goes anywhere near a cart,
        // and snapshot its current price onto the cart line.
        ProductDTO product;
        try {
            ResponseEntity<ProductDTO> productResponse = restTemplate.getForEntity(
                    PRODUCT_SERVICE_URL + "/products/" + productId, ProductDTO.class);
            product = productResponse.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ServiceException("Product not found: " + productId, HttpStatus.BAD_REQUEST);
        } catch (RestClientException e) {
            throw new ServiceException("Product Service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (product == null) {
            throw new ServiceException("Product not found: " + productId, HttpStatus.BAD_REQUEST);
        }

        Optional<Cart> existingCart = cartRepository.findFirstByUserId(userId);

        Cart cart;
        if (existingCart.isPresent()) {
            cart = existingCart.get();
        } else {
            cart = new Cart();
            cart.setId(sequenceGeneratorService.generateSequence("carts_sequence"));
            cart.setUserId(userId);
            cart.setDate(LocalDate.now().toString());
            cart.setProducts(new ArrayList<>());
        }

        List<CartItem> products = cart.getProducts();
        if (products == null) {
            products = new ArrayList<>();
            cart.setProducts(products);
        }

        boolean found = false;
        for (CartItem item : products) {
            if (item.getProductId().equals(productId)) {
                item.setQuantity(item.getQuantity() + quantity);
                // price stays as the snapshot taken when this line was first added
                found = true;
                break;
            }
        }
        if (!found) {
            CartItem newItem = new CartItem();
            newItem.setProductId(productId);
            newItem.setQuantity(quantity);
            newItem.setPrice(product.getPrice());
            products.add(newItem);
        }

        Cart saved = cartRepository.save(cart);

        // Fire-and-forget: a Kafka hiccup should not block the add-to-cart response.
        try {
            cartEventProducer.publishItemAddedToCart(saved.getId(), userId, productId, quantity);
        } catch (Exception e) {
            System.err.println("Failed to publish cart-item-added event for cart "
                    + saved.getId() + ": " + e.getMessage());
        }

        return saved;
    }
}