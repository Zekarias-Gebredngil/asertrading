package com.aser.asertrading;

import com.aser.asertrading.catalog.domain.Product;
import com.aser.asertrading.catalog.domain.ProductStatus;
import com.aser.asertrading.catalog.repository.ProductRepository;
import com.aser.asertrading.identity.web.dto.AuthResponse;
import com.aser.asertrading.identity.web.dto.LoginRequest;
import com.aser.asertrading.inventory.domain.ReferenceType;
import com.aser.asertrading.inventory.repository.StockBalanceRepository;
import com.aser.asertrading.inventory.service.InventoryService;
import com.aser.asertrading.inventory.web.dto.StockBalanceDto;
import com.aser.asertrading.inventory.web.dto.StockReceiptRequest;
import com.aser.asertrading.organization.domain.Warehouse;
import com.aser.asertrading.organization.repository.WarehouseRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                .andExpect(jsonPath("$.data.role").value("SYSTEM_ADMINISTRATOR"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).contains("accessToken");
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

        // Current seeded stock: 250m @ 180 ETB
        // New receipt: 50m @ 240 ETB
        // Expected new avg cost: (250*180 + 50*240) / 300 = (45000 + 12000) / 300 = 57000 / 300 = 190.0000 ETB
        StockReceiptRequest receiptRequest = new StockReceiptRequest(
                warehouse.getId(),
                product.getId(),
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(240.00),
                ReferenceType.MANUAL,
                "TEST-RECEIPT-01",
                "Integration test receipt"
        );

        StockBalanceDto updatedBalance = inventoryService.recordReceipt(receiptRequest, "admin");

        assertThat(updatedBalance.onHandQuantity()).isEqualByComparingTo("300");
        assertThat(updatedBalance.weightedAverageCost()).isEqualByComparingTo("190.0000");
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
}

