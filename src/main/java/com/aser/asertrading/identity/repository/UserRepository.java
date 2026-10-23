package com.aser.asertrading.identity.repository;

import com.aser.asertrading.identity.domain.Role;
import com.aser.asertrading.identity.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Page<User> findByRole(Role role, Pageable pageable);
    Page<User> findByAssignedBranchId(Long branchId, Pageable pageable);
}

