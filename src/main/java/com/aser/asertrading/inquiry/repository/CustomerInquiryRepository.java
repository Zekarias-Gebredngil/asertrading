package com.aser.asertrading.inquiry.repository;

import com.aser.asertrading.inquiry.domain.CustomerInquiry;
import com.aser.asertrading.inquiry.domain.InquiryStatus;
import com.aser.asertrading.inquiry.domain.InquiryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerInquiryRepository extends JpaRepository<CustomerInquiry, Long> {
    Optional<CustomerInquiry> findByInquiryNumber(String inquiryNumber);
    boolean existsByInquiryNumber(String inquiryNumber);
    Page<CustomerInquiry> findByStatus(InquiryStatus status, Pageable pageable);
    Page<CustomerInquiry> findByInquiryType(InquiryType type, Pageable pageable);
}

