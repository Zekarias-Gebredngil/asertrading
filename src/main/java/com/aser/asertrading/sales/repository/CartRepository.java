package com.aser.asertrading.sales.repository;

import com.aser.asertrading.sales.domain.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByCustomerId(Long customerId);
    Optional<Cart> findBySessionId(String sessionId);
}

