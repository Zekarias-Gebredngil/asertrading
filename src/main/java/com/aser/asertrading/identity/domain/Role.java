package com.aser.asertrading.identity.domain;

public enum Role {
    SYSTEM_ADMINISTRATOR,
    COMPANY_MANAGER,
    INVENTORY_MANAGER,
    BRANCH_MANAGER,
    STOREKEEPER,
    PROCUREMENT_OFFICER,
    SALES_OFFICER,
    ACCOUNTANT_AUDITOR,
    WEBSITE_MANAGER,
    CUSTOMER;

    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}

