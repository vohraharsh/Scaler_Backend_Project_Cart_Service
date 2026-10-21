package com.scaler.harsh.cartservice.service;

import com.scaler.harsh.cartservice.dto.CreateCartRequestDTO;
import com.scaler.harsh.cartservice.dto.ProductDTO;
import com.scaler.harsh.cartservice.exceptions.ServiceException;
import com.scaler.harsh.cartservice.kafka.CartEventProducer;
import com.scaler.harsh.cartservice.model.Cart;
import com.scaler.harsh.cartservice.model.CartItem;
import com.scaler.harsh.cartservice.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SelfCartServiceTest {

    private static final String PRODUCT_URL_PREFIX = "http://localhost:8080/products/";

    @Mock
    private CartRepository cartRepository;

    @Mock
    private SequenceGeneratorService sequenceGeneratorService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CartEventProducer cartEventProducer;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private SelfCartService selfCartService;

    @BeforeEach
    void setUp() {
        selfCartService = new SelfCartService(cartRepository, sequenceGeneratorService, restTemplate,
                cartEventProducer, cacheManager);
    }

    private Cart buildCart(Integer id, Integer userId, List<CartItem> products) {
        Cart cart = new Cart();
        cart.setId(id);
        cart.setUserId(userId);
        cart.setDate("2026-10-21");
        cart.setProducts(products);
        return cart;
    }

    private CartItem buildItem(Integer productId, Integer quantity, double price) {
        CartItem item = new CartItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setPrice(price);
        return item;
    }

    private ProductDTO buildProduct(Integer id, double price) {
        ProductDTO dto = new ProductDTO();
        dto.setId(id);
        dto.setPrice(price);
        return dto;
    }

    // ---------- getCartById ----------

    @Test
    void getCartById_found_returnsCart() {
        Cart cart = buildCart(1, 3, Collections.emptyList());
        when(cartRepository.findById(1)).thenReturn(Optional.of(cart));

        Cart result = selfCartService.getCartById(1);

        assertEquals(3, result.getUserId());
    }

    @Test
    void getCartById_notFound_throws404() {
        when(cartRepository.findById(99)).thenReturn(Optional.empty());

        ServiceException ex = assertThrows(ServiceException.class, () -> selfCartService.getCartById(99));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ---------- getAllCarts ----------

    @Test
    void getAllCarts_delegatesToRepository() {
        when(cartRepository.findAll()).thenReturn(Arrays.asList(
                buildCart(1, 3, Collections.emptyList()), buildCart(2, 4, Collections.emptyList())));

        List<Cart> result = selfCartService.getAllCarts();

        assertEquals(2, result.size());
    }

    // ---------- getCartsByUserId ----------

    @Test
    void getCartsByUserId_found_returnsCarts() {
        when(cartRepository.findByUserId(3)).thenReturn(
                Collections.singletonList(buildCart(1, 3, Collections.emptyList())));

        List<Cart> result = selfCartService.getCartsByUserId(3);

        assertEquals(1, result.size());
    }

    @Test
    void getCartsByUserId_empty_throws404() {
        when(cartRepository.findByUserId(99)).thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class, () -> selfCartService.getCartsByUserId(99));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ---------- createCart ----------

    @Test
    void createCart_savesAndEvictsActiveCartCache() {
        CreateCartRequestDTO request = new CreateCartRequestDTO();
        request.setUserId(3);
        request.setProducts(Collections.singletonList(buildItem(1, 2, 100.0)));

        when(sequenceGeneratorService.generateSequence("carts_sequence")).thenReturn(10);
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cacheManager.getCache("carts")).thenReturn(cache);

        Cart result = selfCartService.createCart(request);

        assertEquals(10, result.getId());
        assertEquals(3, result.getUserId());
        verify(cache).evict("active-3");
    }

    // ---------- updateCart ----------

    @Test
    void updateCart_notFound_throws404() {
        when(cartRepository.findById(5)).thenReturn(Optional.empty());

        CreateCartRequestDTO request = new CreateCartRequestDTO();
        ServiceException ex = assertThrows(ServiceException.class, () -> selfCartService.updateCart(5, request));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void updateCart_found_savesAndEvictsCache() {
        Cart existing = buildCart(1, 3, Collections.emptyList());
        when(cartRepository.findById(1)).thenReturn(Optional.of(existing));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cacheManager.getCache("carts")).thenReturn(cache);

        CreateCartRequestDTO request = new CreateCartRequestDTO();
        request.setUserId(3);
        request.setProducts(Collections.singletonList(buildItem(2, 1, 50.0)));

        Cart result = selfCartService.updateCart(1, request);

        assertEquals(1, result.getProducts().size());
        verify(cache).evict("active-3");
    }

    // ---------- deleteCart ----------

    @Test
    void deleteCart_notFound_throws404() {
        when(cartRepository.findById(99)).thenReturn(Optional.empty());

        ServiceException ex = assertThrows(ServiceException.class, () -> selfCartService.deleteCart(99));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(cartRepository, never()).deleteById(any());
    }

    @Test
    void deleteCart_found_deletesAndEvictsCorrectUserCache() {
        Cart existing = buildCart(16, 3, Collections.emptyList());
        when(cartRepository.findById(16)).thenReturn(Optional.of(existing));
        when(cacheManager.getCache("carts")).thenReturn(cache);

        selfCartService.deleteCart(16);

        verify(cartRepository).deleteById(16);
        verify(cache).evict("active-3");
    }

    // ---------- addItemToCart ----------

    @Test
    void addItemToCart_productNotFound_throws400() {
        when(restTemplate.getForEntity(PRODUCT_URL_PREFIX + "9999", ProductDTO.class))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> selfCartService.addItemToCart(3, 9999, 1));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addItemToCart_productServiceUnavailable_throws503() {
        when(restTemplate.getForEntity(PRODUCT_URL_PREFIX + "2", ProductDTO.class))
                .thenThrow(new ResourceAccessException("Connection refused"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> selfCartService.addItemToCart(3, 2, 1));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
    }

    @Test
    void addItemToCart_productResponseBodyNull_throws400() {
        when(restTemplate.getForEntity(PRODUCT_URL_PREFIX + "2", ProductDTO.class))
                .thenReturn(ResponseEntity.ok(null));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> selfCartService.addItemToCart(3, 2, 1));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void addItemToCart_noExistingCart_createsNewCart() {
        when(restTemplate.getForEntity(PRODUCT_URL_PREFIX + "2", ProductDTO.class))
                .thenReturn(ResponseEntity.ok(buildProduct(2, 499.99)));
        when(cartRepository.findFirstByUserId(3)).thenReturn(Optional.empty());
        when(sequenceGeneratorService.generateSequence("carts_sequence")).thenReturn(20);
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cacheManager.getCache("carts")).thenReturn(cache);

        Cart result = selfCartService.addItemToCart(3, 2, 1);

        assertEquals(20, result.getId());
        assertEquals(1, result.getProducts().size());
        assertEquals(499.99, result.getProducts().get(0).getPrice());
        verify(cartEventProducer).publishItemAddedToCart(20, 3, 2, 1);
    }

    @Test
    void addItemToCart_existingCartNewProduct_appendsLine() {
        Cart existing = buildCart(16, 3, new ArrayList<>(Collections.singletonList(buildItem(3, 1, 299.99))));
        when(restTemplate.getForEntity(PRODUCT_URL_PREFIX + "2", ProductDTO.class))
                .thenReturn(ResponseEntity.ok(buildProduct(2, 499.99)));
        when(cartRepository.findFirstByUserId(3)).thenReturn(Optional.of(existing));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cacheManager.getCache("carts")).thenReturn(cache);

        Cart result = selfCartService.addItemToCart(3, 2, 1);

        assertEquals(2, result.getProducts().size());
    }

    @Test
    void addItemToCart_existingProductInCart_mergesQuantityAndKeepsOriginalPriceSnapshot() {
        Cart existing = buildCart(16, 3, new ArrayList<>(Collections.singletonList(buildItem(2, 1, 499.99))));
        // Price has since changed upstream, but the cart line must keep its original snapshot.
        when(restTemplate.getForEntity(PRODUCT_URL_PREFIX + "2", ProductDTO.class))
                .thenReturn(ResponseEntity.ok(buildProduct(2, 599.99)));
        when(cartRepository.findFirstByUserId(3)).thenReturn(Optional.of(existing));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cacheManager.getCache("carts")).thenReturn(cache);

        Cart result = selfCartService.addItemToCart(3, 2, 2);

        assertEquals(1, result.getProducts().size());
        assertEquals(3, result.getProducts().get(0).getQuantity());
        assertEquals(499.99, result.getProducts().get(0).getPrice());
    }

    @Test
    void addItemToCart_evictsActiveCartCacheForUser() {
        when(restTemplate.getForEntity(PRODUCT_URL_PREFIX + "2", ProductDTO.class))
                .thenReturn(ResponseEntity.ok(buildProduct(2, 499.99)));
        when(cartRepository.findFirstByUserId(3)).thenReturn(Optional.empty());
        when(sequenceGeneratorService.generateSequence("carts_sequence")).thenReturn(21);
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cacheManager.getCache("carts")).thenReturn(cache);

        selfCartService.addItemToCart(3, 2, 1);

        verify(cache).evict("active-3");
    }

    @Test
    void addItemToCart_kafkaPublishFails_doesNotPropagateException() {
        when(restTemplate.getForEntity(PRODUCT_URL_PREFIX + "2", ProductDTO.class))
                .thenReturn(ResponseEntity.ok(buildProduct(2, 499.99)));
        when(cartRepository.findFirstByUserId(3)).thenReturn(Optional.empty());
        when(sequenceGeneratorService.generateSequence("carts_sequence")).thenReturn(22);
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cacheManager.getCache("carts")).thenReturn(cache);
        doThrow(new RuntimeException("Kafka broker unreachable"))
                .when(cartEventProducer).publishItemAddedToCart(anyInt(), anyInt(), anyInt(), anyInt());

        // Must not throw — a Kafka hiccup must not fail the add-to-cart response.
        assertDoesNotThrow(() -> selfCartService.addItemToCart(3, 2, 1));
    }

    // ---------- getActiveCartByUserId ----------

    @Test
    void getActiveCartByUserId_found_returnsCart() {
        Cart cart = buildCart(16, 3, Collections.emptyList());
        when(cartRepository.findFirstByUserId(3)).thenReturn(Optional.of(cart));

        Cart result = selfCartService.getActiveCartByUserId(3);

        assertEquals(16, result.getId());
    }

    @Test
    void getActiveCartByUserId_notFound_throws404() {
        when(cartRepository.findFirstByUserId(99)).thenReturn(Optional.empty());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> selfCartService.getActiveCartByUserId(99));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}