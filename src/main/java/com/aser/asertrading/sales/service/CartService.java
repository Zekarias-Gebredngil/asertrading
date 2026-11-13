package com.aser.asertrading.sales.service;

import com.aser.asertrading.catalog.domain.Product;
import com.aser.asertrading.catalog.domain.ProductStatus;
import com.aser.asertrading.catalog.repository.ProductRepository;
import com.aser.asertrading.common.exception.BadRequestException;
import com.aser.asertrading.common.exception.ResourceNotFoundException;
import com.aser.asertrading.sales.domain.Cart;
import com.aser.asertrading.sales.domain.CartItem;
import com.aser.asertrading.sales.repository.CartItemRepository;
import com.aser.asertrading.sales.repository.CartRepository;
import com.aser.asertrading.sales.web.dto.AddToCartRequest;
import com.aser.asertrading.sales.web.dto.CartDto;
import com.aser.asertrading.sales.web.dto.CartItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Cart getOrCreateCartEntity(Long customerId, String sessionId) {
        Cart cart = findCart(customerId, sessionId);
        if (cart == null) {
            cart = Cart.builder()
                    .customerId(customerId)
                    .sessionId(sessionId != null && !sessionId.isBlank() ? sessionId : UUID.randomUUID().toString())
                    .build();
            cart = cartRepository.save(cart);
        }
        return cart;
    }

    @Transactional
    public CartDto getOrCreateCart(Long customerId, String sessionId) {
        return mapToCartDto(getOrCreateCartEntity(customerId, sessionId));
    }

    @Transactional
    public CartDto addToCart(AddToCartRequest request, Long customerId) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        if (!product.isPublished() || product.getStatus() != ProductStatus.ACTIVE) {
            throw new BadRequestException("Product is not eligible for purchase");
        }

        if (product.isQuotationOnly()) {
            throw new BadRequestException("This product requires a custom quotation request and cannot be added to standard cart");
        }

        Cart cart = findCart(customerId, request.sessionId());
        if (cart == null) {
            cart = Cart.builder()
                    .customerId(customerId)
                    .sessionId(request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString())
                    .build();
            cart = cartRepository.save(cart);
        }

        Cart finalCart = cart;
        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseGet(() -> CartItem.builder()
                        .cart(finalCart)
                        .productId(product.getId())
                        .quantity(BigDecimal.ZERO)
                        .unitPrice(product.getSellingPrice())
                        .build());

        cartItem.setQuantity(cartItem.getQuantity().add(request.quantity()));
        cartItem.setUnitPrice(product.getSellingPrice()); // Server-side recalculated price
        cartItem = cartItemRepository.save(cartItem);
        if (cart.getItems() != null && !cart.getItems().contains(cartItem)) {
            cart.getItems().add(cartItem);
        }

        return mapToCartDto(cart);
    }

    @Transactional
    public CartDto updateCartItem(Long itemId, BigDecimal quantity, Long customerId, String sessionId) {
        Cart cart = getOrCreateCartEntity(customerId, sessionId);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new AccessDeniedException("Access denied: Item does not belong to your active cart.");
        }

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            cartItemRepository.delete(item);
        } else {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            item.setQuantity(quantity);
            item.setUnitPrice(product.getSellingPrice());
            cartItemRepository.save(item);
        }

        return mapToCartDto(cart);
    }

    @Transactional
    public CartDto removeCartItem(Long itemId, Long customerId, String sessionId) {
        Cart cart = getOrCreateCartEntity(customerId, sessionId);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new AccessDeniedException("Access denied: Item does not belong to your active cart.");
        }

        cartItemRepository.delete(item);
        return mapToCartDto(cart);
    }

    @Transactional
    public void clearCart(Long cartId) {
        cartItemRepository.deleteByCartId(cartId);
    }

    private Cart findCart(Long customerId, String sessionId) {
        if (customerId != null) {
            return cartRepository.findByCustomerId(customerId).orElse(null);
        }
        if (sessionId != null && !sessionId.isBlank()) {
            return cartRepository.findBySessionId(sessionId).orElse(null);
        }
        return null;
    }

    private CartDto mapToCartDto(Cart cart) {
        List<CartItem> items = cart.getId() != null
                ? cartItemRepository.findByCartId(cart.getId())
                : (cart.getItems() != null ? cart.getItems() : new ArrayList<>());
        Map<Long, Product> productMap = productRepository.findAllById(
                items.stream().map(CartItem::getProductId).toList()
        ).stream().collect(Collectors.toMap(Product::getId, p -> p));

        BigDecimal subtotal = BigDecimal.ZERO;
        List<CartItemDto> itemDtos = new ArrayList<>();

        for (CartItem item : items) {
            Product p = productMap.get(item.getProductId());
            String sku = p != null ? p.getSku() : "UNKNOWN";
            String name = p != null ? p.getName() : "UNKNOWN";
            String img = p != null ? p.getImageUrl() : null;
            BigDecimal lineTotal = item.getQuantity().multiply(item.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineTotal);

            itemDtos.add(new CartItemDto(
                    item.getId(),
                    item.getProductId(),
                    sku,
                    name,
                    img,
                    item.getQuantity(),
                    item.getUnitPrice(),
                    lineTotal
            ));
        }

        return new CartDto(cart.getId(), cart.getSessionId(), itemDtos, subtotal, itemDtos.size());
    }
}

