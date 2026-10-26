package com.aser.asertrading.catalog.service;

import com.aser.asertrading.catalog.domain.ServiceOffering;
import com.aser.asertrading.catalog.repository.ServiceOfferingRepository;
import com.aser.asertrading.catalog.web.dto.CreateServiceOfferingRequest;
import com.aser.asertrading.catalog.web.dto.ServiceItemDto;
import com.aser.asertrading.catalog.web.dto.UpdateServiceOfferingRequest;
import com.aser.asertrading.common.audit.AuditService;
import com.aser.asertrading.common.exception.BadRequestException;
import com.aser.asertrading.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceOfferingService {

    private final ServiceOfferingRepository serviceOfferingRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ServiceItemDto> getActiveServices() {
        return serviceOfferingRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(ServiceItemDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceItemDto> getAllServices() {
        return serviceOfferingRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(ServiceItemDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ServiceItemDto> getAllServices(Pageable pageable) {
        return serviceOfferingRepository.findAllByOrderByDisplayOrderAsc(pageable)
                .map(ServiceItemDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public ServiceItemDto getServiceById(Long id) {
        ServiceOffering offering = serviceOfferingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service offering not found with id: " + id));
        return ServiceItemDto.fromEntity(offering);
    }

    @Transactional
    public ServiceItemDto createService(CreateServiceOfferingRequest request, String actor) {
        String code = request.serviceCode().trim().toUpperCase();
        if (serviceOfferingRepository.existsByCode(code)) {
            throw new BadRequestException("Service offering with code '" + code + "' already exists");
        }

        String caps = request.capabilities() != null ? String.join("; ", request.capabilities()) : null;

        ServiceOffering offering = ServiceOffering.builder()
                .code(code)
                .title(request.title().trim())
                .category(request.category().trim())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .keyOfferings(caps)
                .isQuotationEnabled(request.requiresQuotation() != null ? request.requiresQuotation() : true)
                .isActive(true)
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
                .build();

        offering = serviceOfferingRepository.save(offering);
        auditService.log(actor, "CREATE_SERVICE", "ServiceOffering", offering.getId().toString(),
                "Created service offering: " + offering.getTitle() + " (" + offering.getCode() + ")", null);

        return ServiceItemDto.fromEntity(offering);
    }

    @Transactional
    public ServiceItemDto updateService(Long id, UpdateServiceOfferingRequest request, String actor) {
        ServiceOffering offering = serviceOfferingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service offering not found with id: " + id));

        String caps = request.capabilities() != null ? String.join("; ", request.capabilities()) : null;

        offering.setTitle(request.title().trim());
        offering.setCategory(request.category().trim());
        offering.setDescription(request.description());
        offering.setImageUrl(request.imageUrl());
        offering.setKeyOfferings(caps);
        if (request.requiresQuotation() != null) {
            offering.setQuotationEnabled(request.requiresQuotation());
        }
        if (request.isActive() != null) {
            offering.setActive(request.isActive());
        }
        if (request.displayOrder() != null) {
            offering.setDisplayOrder(request.displayOrder());
        }

        offering = serviceOfferingRepository.save(offering);
        auditService.log(actor, "UPDATE_SERVICE", "ServiceOffering", offering.getId().toString(),
                "Updated service offering: " + offering.getTitle(), null);

        return ServiceItemDto.fromEntity(offering);
    }

    @Transactional
    public void deleteService(Long id, String actor) {
        ServiceOffering offering = serviceOfferingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service offering not found with id: " + id));

        offering.setActive(false);
        serviceOfferingRepository.save(offering);
        auditService.log(actor, "DEACTIVATE_SERVICE", "ServiceOffering", offering.getId().toString(),
                "Deactivated service offering: " + offering.getTitle(), null);
    }
}

