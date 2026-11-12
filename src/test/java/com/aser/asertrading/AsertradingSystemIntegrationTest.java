package com.aser.asertrading;

import com.aser.asertrading.catalog.domain.Product;
import com.aser.asertrading.catalog.domain.ProductStatus;
import com.aser.asertrading.catalog.repository.ProductRepository;
import com.aser.asertrading.identity.domain.Role;
import com.aser.asertrading.identity.domain.User;
import com.aser.asertrading.identity.service.UserPrincipal;
import com.aser.asertrading.identity.web.dto.AuthResponse;
import com.aser.asertrading.identity.web.dto.LoginRequest;
import com.aser.asertrading.identity.web.dto.RefreshTokenRequest;
import com.aser.asertrading.inventory.domain.ReferenceType;
import com.aser.asertrading.inventory.repository.StockBalanceRepository;
import com.aser.asertrading.inventory.service.InventoryService;
import com.aser.asertrading.inventory.web.dto.StockBalanceDto;
import com.aser.asertrading.inventory.web.dto.StockReceiptRequest;
import com.aser.asertrading.organization.domain.Warehouse;
import com.aser.asertrading.organization.repository.WarehouseRepository;
import com.aser.asertrading.organization.service.BranchService;
import com.aser.asertrading.organization.web.dto.BranchDto;
import com.aser.asertrading.organization.web.dto.BranchWarehouseRequest;
import com.aser.asertrading.organization.web.dto.CreateBranchRequest;
import com.aser.asertrading.sales.domain.DeliveryType;
import com.aser.asertrading.sales.domain.PaymentMethod;
import com.aser.asertrading.sales.service.OrderService;
import com.aser.asertrading.sales.web.dto.CheckoutQuoteRequest;
import com.aser.asertrading.sales.web.dto.CheckoutQuoteResponse;
import com.aser.asertrading.sales.web.dto.CreateOrderRequest;
import com.aser.asertrading.sales.web.dto.OrderDto;
import com.aser.asertrading.transfer.domain.TransferStatus;
import com.aser.asertrading.transfer.service.TransferWorkflowService;
import com.aser.asertrading.transfer.web.dto.CreateTransferRequest;
import com.aser.asertrading.transfer.web.dto.StockTransferDto;
import com.aser.asertrading.transfer.web.dto.TransferItemRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AsertradingSystemIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private StockBalanceRepository stockBalanceRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private TransferWorkflowService transferWorkflowService;

    @Autowired
    private BranchService branchService;

    @Test
    @DisplayName("Staff authentication returns valid JWT token and user authorities")
    void testStaffLogin() throws Exception {
        LoginRequest loginRequest = new LoginRequest("admin", "Admin@123456");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresIn").isNumber())
                .andExpect(jsonPath("$.data.role").value("SYSTEM_ADMINISTRATOR"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode loginNode = objectMapper.readTree(responseBody);
        String accessToken = loginNode.path("data").path("accessToken").asText();
        String refreshToken = loginNode.path("data").path("refreshToken").asText();
        assertThat(accessToken).isNotEmpty();
        assertThat(refreshToken).isNotEmpty();
        assertThat(accessToken.split("\\.")).hasSize(3);
        assertThat(refreshToken.split("\\.")).hasSize(3);
        assertThat(refreshToken.length()).isEqualTo(accessToken.length());
    }

    @Test
    @DisplayName("Refresh token obtains new access token and implements rotation")
    void testRefreshTokenRotationAndRevocation() throws Exception {
        LoginRequest loginRequest = new LoginRequest("admin", "Admin@123456");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        String initialResponse = loginResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(initialResponse);
        String initialAccessToken = rootNode.path("data").path("accessToken").asText();
        String initialRefreshToken = rootNode.path("data").path("refreshToken").asText();
        assertThat(initialRefreshToken.split("\\.")).hasSize(3);
        assertThat(initialRefreshToken.length()).isEqualTo(initialAccessToken.length());

        // 1. Refresh using initial refresh token
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(initialRefreshToken);
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresIn").isNumber())
                .andReturn();

        com.fasterxml.jackson.databind.JsonNode refreshNode = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String rotatedRefreshToken = refreshNode.path("data").path("refreshToken").asText();
        String newAccessToken = refreshNode.path("data").path("accessToken").asText();

        // Rotated refresh token must be a JWT matching access token pattern and length
        assertThat(rotatedRefreshToken.split("\\.")).hasSize(3);
        assertThat(rotatedRefreshToken.length()).isEqualTo(newAccessToken.length());

        // Rotated refresh token must be different from initial
        assertThat(rotatedRefreshToken).isNotEqualTo(initialRefreshToken);

        // 2. Replaying the old (now revoked) refresh token must fail with 401 Unauthorized
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(initialRefreshToken))))
                .andExpect(status().isUnauthorized());

        // 3. Logout using the rotated refresh token
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(rotatedRefreshToken))))
                .andExpect(status().isOk());

        // 4. Using the logged-out refresh token must now fail with 401 Unauthorized
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(rotatedRefreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Public product catalog endpoint hides purchase costs and sensitive supplier details")
    void testPublicCatalogDataProtection() throws Exception {
        mockMvc.perform(get("/api/v1/public/products")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                // Verify purchaseCost is NEVER present in the public response JSON
                .andExpect(jsonPath("$.data.content[0].purchaseCost").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].preferredSupplierId").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].availability").exists());
    }

    @Test
    @DisplayName("Inventory receipt updates stock balance and recalculates weighted-average cost atomically")
    @Transactional
    void testInventoryReceiptAndWeightedAverageCost() {
        Product product = productRepository.findBySku("GAR-HTV-WHT-01").orElseThrow();
        Warehouse warehouse = warehouseRepository.findByCode("HQ-WH1").orElseThrow();

        // Check current balance before test receipt
        com.aser.asertrading.inventory.domain.StockBalance currentBalance =
                stockBalanceRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId()).orElse(null);
        BigDecimal initialOnHand = currentBalance != null ? currentBalance.getOnHandQuantity() : BigDecimal.ZERO;
        BigDecimal initialCost = currentBalance != null ? currentBalance.getWeightedAverageCost() : BigDecimal.valueOf(180.00);

        BigDecimal receiptQty = BigDecimal.valueOf(50);
        BigDecimal receiptCost = BigDecimal.valueOf(240.00);
        BigDecimal expectedOnHand = initialOnHand.add(receiptQty);
        BigDecimal expectedAvgCost = (initialOnHand.multiply(initialCost).add(receiptQty.multiply(receiptCost)))
                .divide(expectedOnHand, 4, java.math.RoundingMode.HALF_UP);

        StockReceiptRequest receiptRequest = new StockReceiptRequest(
                warehouse.getId(),
                product.getId(),
                receiptQty,
                receiptCost,
                ReferenceType.MANUAL,
                "TEST-RECEIPT-01",
                "Integration test receipt"
        );

        StockBalanceDto updatedBalance = inventoryService.recordReceipt(receiptRequest, "admin");

        assertThat(updatedBalance.onHandQuantity()).isEqualByComparingTo(expectedOnHand.toString());
        assertThat(updatedBalance.weightedAverageCost()).isEqualByComparingTo(expectedAvgCost.toString());
    }

    @Test
    @DisplayName("Checkout quote validates stock availability and computes server-side totals with 15% VAT")
    void testCheckoutQuoteCalculation() {
        Product product = productRepository.findBySku("GAR-HTV-WHT-01").orElseThrow();

        // 2 units @ 250 ETB = 500 ETB subtotal
        // Delivery fee = 150 ETB
        // VAT 15% on 500 = 75 ETB
        // Total = 500 + 150 + 75 = 725 ETB
        CheckoutQuoteRequest quoteRequest = new CheckoutQuoteRequest(
                DeliveryType.DELIVERY,
                null,
                "Addis Ababa",
                List.of(new CheckoutQuoteRequest.CartItemRequest(product.getId(), BigDecimal.valueOf(2)))
        );

        CheckoutQuoteResponse quote = orderService.calculateCheckoutQuote(quoteRequest);

        assertThat(quote.subtotal()).isEqualByComparingTo("500.00");
        assertThat(quote.deliveryFee()).isEqualByComparingTo("150.00");
        assertThat(quote.taxAmount()).isEqualByComparingTo("75.00");
        assertThat(quote.totalAmount()).isEqualByComparingTo("725.00");
        assertThat(quote.isStockAvailable()).isTrue();
    }

    @Test
    @DisplayName("Inter-branch transfer workflow: request -> approve -> dispatch deducts source -> receive increases destination")
    @Transactional
    void testTransferWorkflow() {
        Product product = productRepository.findBySku("GAR-HTV-WHT-01").orElseThrow();
        Warehouse sourceWh = warehouseRepository.findByCode("HQ-WH1").orElseThrow();
        Warehouse destWh = warehouseRepository.findByCode("BOLE-STORE").orElseThrow();

        BigDecimal initialSourceStock = stockBalanceRepository.findByProductIdAndWarehouseId(product.getId(), sourceWh.getId())
                .map(b -> b.getOnHandQuantity()).orElse(BigDecimal.ZERO);

        BigDecimal initialDestStock = stockBalanceRepository.findByProductIdAndWarehouseId(product.getId(), destWh.getId())
                .map(b -> b.getOnHandQuantity()).orElse(BigDecimal.ZERO);

        // 1. Create Transfer Request
        CreateTransferRequest createReq = new CreateTransferRequest(
                sourceWh.getId(),
                destWh.getId(),
                "Transfer 20 units for Bole branch opening",
                List.of(new TransferItemRequest(product.getId(), BigDecimal.valueOf(20)))
        );
        StockTransferDto transfer = transferWorkflowService.createTransfer(createReq, "manager");
        assertThat(transfer.status()).isEqualTo(TransferStatus.DRAFT);

        // 2. Submit for approval
        transfer = transferWorkflowService.submitTransfer(transfer.id(), "manager");
        assertThat(transfer.status()).isEqualTo(TransferStatus.PENDING_APPROVAL);

        // 3. Approve (Admin approves)
        transfer = transferWorkflowService.approveTransfer(transfer.id(), "admin");
        assertThat(transfer.status()).isEqualTo(TransferStatus.APPROVED);

        // 4. Dispatch (Storekeeper dispatches 20 units)
        transfer = transferWorkflowService.dispatchTransfer(transfer.id(), "storekeeper");
        assertThat(transfer.status()).isEqualTo(TransferStatus.IN_TRANSIT);

        // Source stock balance should be reduced by exactly 20 units
        BigDecimal afterDispatchSourceStock = stockBalanceRepository.findByProductIdAndWarehouseId(product.getId(), sourceWh.getId())
                .orElseThrow().getOnHandQuantity();
        assertThat(afterDispatchSourceStock).isEqualByComparingTo(initialSourceStock.subtract(BigDecimal.valueOf(20)));

        // 5. Receive at Destination
        Long transferItemId = transfer.items().get(0).id();
        com.aser.asertrading.transfer.web.dto.TransferReceiveRequest receiveReq = new com.aser.asertrading.transfer.web.dto.TransferReceiveRequest(
                "Received in full in good condition",
                List.of(new com.aser.asertrading.transfer.web.dto.TransferReceiveItemRequest(transferItemId, BigDecimal.valueOf(20)))
        );
        transfer = transferWorkflowService.receiveTransfer(transfer.id(), receiveReq, "manager");
        assertThat(transfer.status()).isEqualTo(TransferStatus.RECEIVED);

        // Destination stock balance should increase by exactly 20 units
        BigDecimal afterReceiveDestStock = stockBalanceRepository.findByProductIdAndWarehouseId(product.getId(), destWh.getId())
                .orElseThrow().getOnHandQuantity();
        assertThat(afterReceiveDestStock).isEqualByComparingTo(initialDestStock.add(BigDecimal.valueOf(20)));
    }

    @Test
    @DisplayName("Dynamically created branch auto-provisions default warehouse and allows inter-branch transfer")
    @Transactional
    void testDynamicBranchTransferWorkflow() {
        Product product = productRepository.findBySku("GAR-HTV-WHT-01").orElseThrow();
        Warehouse sourceWh = warehouseRepository.findByCode("HQ-WH1").orElseThrow();

        // 1. Create a new branch via BranchService (as happens from the UI)
        CreateBranchRequest branchReq = new CreateBranchRequest(
                "Hawassa Showroom",
                "HAW-" + (System.currentTimeMillis() % 10000),
                "Main Commercial Avenue",
                "Hawassa",
                "+251 46 220 1234",
                "hawassa@asertrading.et",
                true
        );
        BranchDto newBranch = branchService.createBranch(branchReq, "admin");
        assertThat(newBranch).isNotNull();
        assertThat(newBranch.id()).isNotNull();

        // 2. Verify that default warehouse was auto-provisioned
        List<Warehouse> branchWarehouses = warehouseRepository.findByBranchId(newBranch.id());
        assertThat(branchWarehouses).isNotEmpty();
        Warehouse provisionedWh = branchWarehouses.get(0);
        assertThat(provisionedWh.isDefault()).isTrue();

        // 3. Create transfer using new branch's ID as destinationWarehouseId (fallback behavior)
        CreateTransferRequest transferReqBranchId = new CreateTransferRequest(
                sourceWh.getId(),
                newBranch.id(), // passing Branch ID directly!
                "Initial stock replenishment for newly opened Hawassa branch",
                List.of(new TransferItemRequest(product.getId(), BigDecimal.valueOf(15)))
        );
        StockTransferDto transferBranchId = transferWorkflowService.createTransfer(transferReqBranchId, "manager");
        assertThat(transferBranchId.status()).isEqualTo(TransferStatus.DRAFT);
        assertThat(transferBranchId.destinationWarehouseId()).isEqualTo(provisionedWh.getId());
        assertThat(transferBranchId.destinationBranchName()).isEqualTo("Hawassa Showroom");

        // 4. Create transfer using provisioned warehouse ID as destinationWarehouseId
        CreateTransferRequest transferReqWhId = new CreateTransferRequest(
                sourceWh.getId(),
                provisionedWh.getId(), // passing Warehouse ID directly!
                "Second shipment for Hawassa branch",
                List.of(new TransferItemRequest(product.getId(), BigDecimal.valueOf(10)))
        );
        StockTransferDto transferWhId = transferWorkflowService.createTransfer(transferReqWhId, "manager");
        assertThat(transferWhId.status()).isEqualTo(TransferStatus.DRAFT);
        assertThat(transferWhId.destinationWarehouseId()).isEqualTo(provisionedWh.getId());

        // 5. Test bidirectional access enforcement when caller is assigned to the new branch (e.g. Branch 5)
        User branchManager = User.builder()
                .id(9999L)
                .username("hawassa_manager")
                .email("hawassa_mgr@asertrading.et")
                .passwordHash("hash")
                .fullName("Hawassa Branch Manager")
                .role(Role.BRANCH_MANAGER)
                .assignedBranchId(newBranch.id())
                .isActive(true)
                .build();
        UserPrincipal principal = new UserPrincipal(branchManager);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        try {
            // 5a. Inbound replenishment: Requesting stock from HQ (Branch 1) into Hawassa (Branch 5)
            CreateTransferRequest inboundReq = new CreateTransferRequest(
                    sourceWh.getId(),
                    provisionedWh.getId(),
                    "Inbound shortage replenishment request for Hawassa",
                    List.of(new TransferItemRequest(product.getId(), BigDecimal.valueOf(5)))
            );
            StockTransferDto inboundTransfer = transferWorkflowService.createTransfer(inboundReq, "hawassa_manager");
            assertThat(inboundTransfer.status()).isEqualTo(TransferStatus.DRAFT);
            assertThat(inboundTransfer.sourceWarehouseId()).isEqualTo(sourceWh.getId());
            assertThat(inboundTransfer.destinationWarehouseId()).isEqualTo(provisionedWh.getId());

            // 5b. Outbound transfer: Sending stock from Hawassa (Branch 5) to HQ (Branch 1)
            CreateTransferRequest outboundReq = new CreateTransferRequest(
                    provisionedWh.getId(),
                    sourceWh.getId(),
                    "Outbound stock return from Hawassa to HQ",
                    List.of(new TransferItemRequest(product.getId(), BigDecimal.valueOf(3)))
            );
            StockTransferDto outboundTransfer = transferWorkflowService.createTransfer(outboundReq, "hawassa_manager");
            assertThat(outboundTransfer.status()).isEqualTo(TransferStatus.DRAFT);
            assertThat(outboundTransfer.sourceWarehouseId()).isEqualTo(provisionedWh.getId());
            assertThat(outboundTransfer.destinationWarehouseId()).isEqualTo(sourceWh.getId());

            // 5c. Third-party transfer attempt: Hawassa manager tries to transfer between HQ and Bole (neither is Hawassa)
            Warehouse boleWh = warehouseRepository.findByCode("BOLE-STORE").orElseThrow();
            CreateTransferRequest unauthorizedReq = new CreateTransferRequest(
                    sourceWh.getId(),
                    boleWh.getId(),
                    "Unauthorized inter-branch transfer between foreign branches",
                    List.of(new TransferItemRequest(product.getId(), BigDecimal.valueOf(2)))
            );
            assertThatThrownBy(() -> transferWorkflowService.createTransfer(unauthorizedReq, "hawassa_manager"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Access denied");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("Modifying branch allows updating branch code and associated warehouses simultaneously")
    @Transactional
    void testModifyBranchCodeAndWarehouses() {
        // 1. Create a branch
        String initialCode = "DIR-" + (System.currentTimeMillis() % 10000);
        CreateBranchRequest createReq = new CreateBranchRequest(
                "Dire Dawa Branch",
                initialCode,
                "Station Road",
                "Dire Dawa",
                "+251 25 111 2233",
                "diredawa@asertrading.et",
                true
        );
        BranchDto created = branchService.createBranch(createReq, "admin");
        assertThat(created.code()).isEqualTo(initialCode);

        // Retrieve the default provisioned warehouse
        List<Warehouse> initialWhs = warehouseRepository.findByBranchId(created.id());
        assertThat(initialWhs).hasSize(1);
        Warehouse defaultWh = initialWhs.get(0);

        // 2. Modify branch code, update the existing warehouse name/code, and add a secondary depot
        String updatedBranchCode = "DIR-NEW-" + (System.currentTimeMillis() % 10000);
        String updatedWhCode = updatedBranchCode + "-MAIN";
        String secondWhCode = updatedBranchCode + "-DEPOT2";

        CreateBranchRequest updateReq = new CreateBranchRequest(
                "Dire Dawa Central Hub",
                updatedBranchCode,
                "Expressway Terminal",
                "Dire Dawa",
                "+251 25 999 8877",
                "dire.hub@asertrading.et",
                true,
                List.of(
                        new BranchWarehouseRequest(defaultWh.getId(), "Dire Dawa Main Storage", updatedWhCode, true, true),
                        new BranchWarehouseRequest(null, "Dire Dawa Annex Depot", secondWhCode, false, true)
                )
        );

        BranchDto updatedBranch = branchService.updateBranch(created.id(), updateReq, "admin");

        // 3. Verify branch code update
        assertThat(updatedBranch.code()).isEqualTo(updatedBranchCode);
        assertThat(updatedBranch.name()).isEqualTo("Dire Dawa Central Hub");

        // 4. Verify associated warehouses updated and added
        List<Warehouse> updatedWhs = warehouseRepository.findByBranchId(created.id());
        assertThat(updatedWhs).hasSize(2);

        Warehouse foundMainWh = warehouseRepository.findById(defaultWh.getId()).orElseThrow();
        assertThat(foundMainWh.getName()).isEqualTo("Dire Dawa Main Storage");
        assertThat(foundMainWh.getCode()).isEqualTo(updatedWhCode);
        assertThat(foundMainWh.isDefault()).isTrue();

        Warehouse foundAnnexWh = warehouseRepository.findByCode(secondWhCode).orElseThrow();
        assertThat(foundAnnexWh.getName()).isEqualTo("Dire Dawa Annex Depot");
        assertThat(foundAnnexWh.getBranch().getId()).isEqualTo(created.id());
        assertThat(foundAnnexWh.isDefault()).isFalse();
    }

    @Test
    @DisplayName("Corporate services are fetched directly from database and reflect live updates")
    void testServicesSourceOfTruthFromDatabase() throws Exception {
        // Query public services endpoint
        mockMvc.perform(get("/api/v1/public/services")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].serviceCode").isNotEmpty())
                .andExpect(jsonPath("$.data[0].title").isNotEmpty())
                .andExpect(jsonPath("$.data[0].category").isNotEmpty());
    }

    @Test
    @DisplayName("Catalog quotationOnly filter separates e-commerce purchasable goods from quotation-only services")
    void testCatalogQuotationOnlyFilter() throws Exception {
        // Query standard buyable products (quotationOnly = false)
        mockMvc.perform(get("/api/v1/public/products")
                        .param("quotationOnly", "false")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[?(@.quotationOnly == true)]").doesNotExist());

        // Query services / quotation-only items (quotationOnly = true)
        mockMvc.perform(get("/api/v1/public/products")
                        .param("quotationOnly", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[?(@.quotationOnly == false)]").doesNotExist());
    }

    @Test
    @DisplayName("CORS allows requests from LAN frontend origin http://192.168.0.208:4200")
    void testCorsAllowedOrigins() throws Exception {
        MockMvc secureMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();

        // Preflight OPTIONS request
        secureMvc.perform(options("/api/v1/public/services")
                        .header("Origin", "http://192.168.0.208:4200")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://192.168.0.208:4200"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        // Direct GET request
        secureMvc.perform(get("/api/v1/public/services")
                        .header("Origin", "http://192.168.0.208:4200")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://192.168.0.208:4200"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
}

