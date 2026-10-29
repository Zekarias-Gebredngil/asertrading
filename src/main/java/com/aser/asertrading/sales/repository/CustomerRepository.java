package com.aser.asertrading.sales.repository;

import com.aser.asertrading.sales.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByUserId(Long userId);
    Optional<Customer> findByPhone(String phone);
}

