package com.aser.asertrading.identity.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_branch_access", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_branch", columnNames = {"user_id", "branch_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBranchAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;
}

