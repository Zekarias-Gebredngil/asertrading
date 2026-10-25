package com.aser.asertrading.organization.web;

import com.aser.asertrading.common.dto.ApiResponse;
import com.aser.asertrading.organization.service.BranchService;
import com.aser.asertrading.organization.web.dto.BranchDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/branches")
@RequiredArgsConstructor
@Tag(name = "Public Branches", description = "Public branch directory for customer store locator")
public class PublicBranchController {

    private final BranchService branchService;

    @GetMapping
    @Operation(summary = "Get list of active company branches for public website")
    public ApiResponse<List<BranchDto>> getPublicBranches() {
        return ApiResponse.success(branchService.getActiveBranches());
    }
}

