package com.aser.asertrading.identity.repository;

import com.aser.asertrading.identity.domain.UserBranchAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBranchAccessRepository extends JpaRepository<UserBranchAccess, Long> {
    List<UserBranchAccess> findByUserId(Long userId);
    boolean existsByUserIdAndBranchId(Long userId, Long branchId);
    void deleteByUserIdAndBranchId(Long userId, Long branchId);
}

