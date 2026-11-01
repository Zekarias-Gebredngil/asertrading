package com.aser.asertrading.setting.repository;

import com.aser.asertrading.setting.domain.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {
    Optional<SystemSetting> findFirstByOrderByIdAsc();
}

