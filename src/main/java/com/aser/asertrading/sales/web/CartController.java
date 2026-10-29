package com.aser.asertrading.sales.web;

import com.aser.asertrading.common.dto.ApiResponse;
import com.aser.asertrading.identity.service.UserPrincipal;
import com.aser.asertrading.sales.service.CartService;
import com.aser.asertrading.sales.web.dto.AddToCartRequest;
import com.aser.asertrading.sales.web.dto.CartDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Shopping Cart", description = "Endpoints for managing customer shopping cart and server-side pricing")
public class CartController {

    private final CartService cartService;

    @GetMapping("/items")
    @Operation(summary = "Get current cart contents with server-side recalculated subtotals")
    public ApiResponse<CartDto> getCart(
            @RequestParam(required = false) String sessionId,
            Authentication authentication) {

        Long customerId = extractCustomerId(authentication);
        return ApiResponse.success(cartService.getOrCreateCart(customerId, sessionId));
    }

    @PostMapping("/items")
    @Operation(summary = "Add an eligible product to shopping cart")
    public ApiResponse<CartDto> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            Authentication authentication) {

        Long customerId = extractCustomerId(authentication);
        return ApiResponse.success("Item added to cart", cartService.addToCart(request, customerId));
    }

    @PatchMapping("/items/{id}")
    @Operation(summary = "Update cart item quantity")
    public ApiResponse<CartDto> updateCartItem(
            @PathVariable Long id,
            @RequestParam BigDecimal quantity,
            @RequestParam(required = false) String sessionId,
            Authentication authentication) {

        Long customerId = extractCustomerId(authentication);
        return ApiResponse.success("Cart updated", cartService.updateCartItem(id, quantity, customerId, sessionId));
    }

    @DeleteMapping("/items/{id}")
    @Operation(summary = "Remove item from cart")
    public ApiResponse<CartDto> removeCartItem(
            @PathVariable Long id,
            @RequestParam(required = false) String sessionId,
            Authentication authentication) {

        Long customerId = extractCustomerId(authentication);
        return ApiResponse.success("Item removed from cart", cartService.removeCartItem(id, customerId, sessionId));
    }

    private Long extractCustomerId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }
}

