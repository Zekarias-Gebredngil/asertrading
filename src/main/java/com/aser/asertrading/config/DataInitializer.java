package com.aser.asertrading.config;

import com.aser.asertrading.catalog.domain.Product;
import com.aser.asertrading.catalog.domain.ProductCategory;
import com.aser.asertrading.catalog.domain.ProductStatus;
import com.aser.asertrading.catalog.domain.ServiceOffering;
import com.aser.asertrading.catalog.domain.UnitOfMeasure;
import com.aser.asertrading.catalog.repository.ProductCategoryRepository;
import com.aser.asertrading.catalog.repository.ProductRepository;
import com.aser.asertrading.catalog.repository.ServiceOfferingRepository;
import com.aser.asertrading.catalog.repository.UnitOfMeasureRepository;
import com.aser.asertrading.identity.domain.Role;
import com.aser.asertrading.identity.domain.User;
import com.aser.asertrading.identity.domain.UserBranchAccess;
import com.aser.asertrading.identity.repository.UserBranchAccessRepository;
import com.aser.asertrading.identity.repository.UserRepository;
import com.aser.asertrading.inventory.domain.ReferenceType;
import com.aser.asertrading.inventory.domain.StockBalance;
import com.aser.asertrading.inventory.domain.StockMovement;
import com.aser.asertrading.inventory.domain.StockMovementType;
import com.aser.asertrading.inventory.repository.StockBalanceRepository;
import com.aser.asertrading.inventory.repository.StockMovementRepository;
import com.aser.asertrading.organization.domain.Branch;
import com.aser.asertrading.organization.domain.Warehouse;
import com.aser.asertrading.organization.repository.BranchRepository;
import com.aser.asertrading.organization.repository.WarehouseRepository;
import com.aser.asertrading.procurement.domain.Supplier;
import com.aser.asertrading.procurement.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserBranchAccessRepository userBranchAccessRepository;
    private final BranchRepository branchRepository;
    private final WarehouseRepository warehouseRepository;
    private final UnitOfMeasureRepository unitRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final SupplierRepository supplierRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedServicesIfEmpty();

        if (userRepository.count() > 0) {
            log.info("Database baseline data already initialized.");
            return;
        }

        log.info("Seeding initial Aser Trading platform baseline data...");

        // 1. Branches
        Branch hq = branchRepository.save(Branch.builder()
                .name("Aser Headquarters & Main Plant")
                .code("HQ")
                .address("Gotera Industrial Zone, Building 4")
                .city("Addis Ababa")
                .phone("+251 11 465 1200")
                .email("hq@asertrading.com")
                .isActive(true)
                .build());

        Branch bole = branchRepository.save(Branch.builder()
                .name("Bole Showroom & Retail Branch")
                .code("BOLE")
                .address("Bole Medhanialem, Mega Mall Ground Floor")
                .city("Addis Ababa")
                .phone("+251 11 662 3400")
                .email("bole@asertrading.com")
                .isActive(true)
                .build());

        Branch merkato = branchRepository.save(Branch.builder()
                .name("Merkato Garment Materials Hub")
                .code("MERKATO")
                .address("Merkato, Military Tera, Block B")
                .city("Addis Ababa")
                .phone("+251 11 278 8800")
                .email("merkato@asertrading.com")
                .isActive(true)
                .build());

        // 2. Warehouses
        Warehouse hqWh = warehouseRepository.save(Warehouse.builder()
                .branch(hq)
                .name("HQ Central Production Warehouse")
                .code("HQ-WH1")
                .isDefault(true)
                .isActive(true)
                .build());

        Warehouse boleWh = warehouseRepository.save(Warehouse.builder()
                .branch(bole)
                .name("Bole Retail Store")
                .code("BOLE-STORE")
                .isDefault(true)
                .isActive(true)
                .build());

        Warehouse merkatoWh = warehouseRepository.save(Warehouse.builder()
                .branch(merkato)
                .name("Merkato Bulk Depot")
                .code("MERKATO-DEPOT")
                .isDefault(true)
                .isActive(true)
                .build());

        // 3. Staff Users
        User admin = userRepository.save(User.builder()
                .username("admin")
                .email("admin@asertrading.com")
                .fullName("System Administrator")
                .role(Role.SYSTEM_ADMINISTRATOR)
                .assignedBranchId(hq.getId())
                .passwordHash(passwordEncoder.encode("Admin@123456"))
                .isActive(true)
                .build());

        User manager = userRepository.save(User.builder()
                .username("manager")
                .email("manager@asertrading.com")
                .fullName("Bole Branch Manager")
                .role(Role.BRANCH_MANAGER)
                .assignedBranchId(bole.getId())
                .passwordHash(passwordEncoder.encode("Manager@123456"))
                .isActive(true)
                .build());

        User storekeeper = userRepository.save(User.builder()
                .username("storekeeper")
                .email("storekeeper@asertrading.com")
                .fullName("HQ Storekeeper")
                .role(Role.STOREKEEPER)
                .assignedBranchId(hq.getId())
                .passwordHash(passwordEncoder.encode("Store@123456"))
                .isActive(true)
                .build());

        User sales = userRepository.save(User.builder()
                .username("sales")
                .email("sales@asertrading.com")
                .fullName("Aser Sales Representative")
                .role(Role.SALES_OFFICER)
                .assignedBranchId(bole.getId())
                .passwordHash(passwordEncoder.encode("Sales@123456"))
                .isActive(true)
                .build());

        User auditor = userRepository.save(User.builder()
                .username("auditor")
                .email("auditor@asertrading.com")
                .fullName("Corporate Financial Auditor")
                .role(Role.ACCOUNTANT_AUDITOR)
                .passwordHash(passwordEncoder.encode("Auditor@123456"))
                .isActive(true)
                .build());

        User director = userRepository.save(User.builder()
                .username("director")
                .email("director@asertrading.com")
                .fullName("Managing Director")
                .role(Role.COMPANY_MANAGER)
                .passwordHash(passwordEncoder.encode("Director@123456"))
                .isActive(true)
                .build());

        userBranchAccessRepository.save(UserBranchAccess.builder().userId(admin.getId()).branchId(hq.getId()).build());
        userBranchAccessRepository.save(UserBranchAccess.builder().userId(admin.getId()).branchId(bole.getId()).build());
        userBranchAccessRepository.save(UserBranchAccess.builder().userId(admin.getId()).branchId(merkato.getId()).build());
        userBranchAccessRepository.save(UserBranchAccess.builder().userId(manager.getId()).branchId(bole.getId()).build());
        userBranchAccessRepository.save(UserBranchAccess.builder().userId(storekeeper.getId()).branchId(hq.getId()).build());
        userBranchAccessRepository.save(UserBranchAccess.builder().userId(sales.getId()).branchId(bole.getId()).build());

        // 4. Units of Measure
        UnitOfMeasure uomPcs = unitRepository.save(UnitOfMeasure.builder().name("Piece").code("PCS").symbol("pcs").isBaseUnit(true).build());
        UnitOfMeasure uomMeter = unitRepository.save(UnitOfMeasure.builder().name("Meter").code("M").symbol("m").isBaseUnit(true).build());
        UnitOfMeasure uomRoll = unitRepository.save(UnitOfMeasure.builder().name("Roll").code("ROLL").symbol("roll").isBaseUnit(false).build());
        UnitOfMeasure uomKg = unitRepository.save(UnitOfMeasure.builder().name("Kilogram").code("KG").symbol("kg").isBaseUnit(true).build());
        UnitOfMeasure uomPack = unitRepository.save(UnitOfMeasure.builder().name("Pack").code("PACK").symbol("pk").isBaseUnit(false).build());

        // 5. Product Categories
        ProductCategory printPressCat = categoryRepository.save(ProductCategory.builder()
                .name("Printing Press Services")
                .code("PRINT-PRESS")
                .slug("printing-press-services")
                .description("Commercial book, catalog, brochure, packaging and marketing collateral printing")
                .isActive(true)
                .build());

        ProductCategory brandingCat = categoryRepository.save(ProductCategory.builder()
                .name("Branding & Promotion")
                .code("BRAND-PROMO")
                .slug("branding-promotion")
                .description("Indoor and outdoor corporate branding, promotional gifts, signage and banners")
                .isActive(true)
                .build());

        ProductCategory garmentCat = categoryRepository.save(ProductCategory.builder()
                .name("Garment Materials")
                .code("GARMENT-MAT")
                .slug("garment-materials")
                .description("Fabrics, heat transfer vinyl, interlining, sewing threads and garment accessories")
                .isActive(true)
                .build());

        ProductCategory printMatCat = categoryRepository.save(ProductCategory.builder()
                .name("Printing Materials & Equipment")
                .code("PRINT-MAT")
                .slug("printing-materials-equipment")
                .description("Industrial offset plates, UV inks, banner rolls and press equipment distribution")
                .isActive(true)
                .build());

        // 6. Suppliers
        Supplier premierSupplier = supplierRepository.save(Supplier.builder()
                .name("Ethiopian Textile & Paper Mills")
                .code("SUP-ET-PAPER")
                .contactPerson("Abebe Bekele")
                .email("sales@etpaper.et")
                .phone("+251 11 551 2233")
                .city("Addis Ababa")
                .isActive(true)
                .build());

        // 7. Products
        Product htvWhite = productRepository.save(Product.builder()
                .sku("GAR-HTV-WHT-01")
                .barcode("600123450001")
                .name("Heat Transfer Vinyl (HTV) Roll - White (25m)")
                .slug("heat-transfer-vinyl-roll-white-25m")
                .description("High-durability polyurethane heat transfer vinyl for garment branding and t-shirt graphics.")
                .category(garmentCat)
                .baseUnit(uomMeter)
                .purchaseCost(BigDecimal.valueOf(180.00))
                .sellingPrice(BigDecimal.valueOf(250.00))
                .reorderThreshold(BigDecimal.valueOf(50))
                .preferredSupplierId(premierSupplier.getId())
                .status(ProductStatus.ACTIVE)
                .isQuotationOnly(false)
                .isPublished(true)
                .imageUrl("/assets/images/htv-white.jpg")
                .specifications("Width: 50cm, Length: 25m, Transfer Temp: 150-160C, Time: 10-15s")
                .build());

        Product offsetPaper = productRepository.save(Product.builder()
                .sku("PRN-PAP-A4-80G")
                .barcode("600123450002")
                .name("Premium Offset Art Paper 80gsm (Pack of 500)")
                .slug("premium-offset-art-paper-80gsm")
                .description("Coated woodfree smooth art paper engineered for high-definition color printing press jobs.")
                .category(printMatCat)
                .baseUnit(uomPack)
                .purchaseCost(BigDecimal.valueOf(450.00))
                .sellingPrice(BigDecimal.valueOf(620.00))
                .reorderThreshold(BigDecimal.valueOf(20))
                .preferredSupplierId(premierSupplier.getId())
                .status(ProductStatus.ACTIVE)
                .isQuotationOnly(false)
                .isPublished(true)
                .imageUrl("/assets/images/art-paper.jpg")
                .specifications("Weight: 80 GSM, Brightness: 98%, Sheet Size: A4 (210 x 297 mm)")
                .build());

        Product customMagazine = productRepository.save(Product.builder()
                .sku("SRV-PRN-MAGAZINE")
                .name("Custom Corporate Magazine & Catalog Printing")
                .slug("custom-corporate-magazine-printing")
                .description("Full-color bespoke magazines with saddle stitching or perfect binding for corporate publications.")
                .category(printPressCat)
                .baseUnit(uomPcs)
                .purchaseCost(BigDecimal.ZERO)
                .sellingPrice(BigDecimal.ZERO)
                .reorderThreshold(BigDecimal.ZERO)
                .status(ProductStatus.ACTIVE)
                .isQuotationOnly(true) // Quotation only per Decision #10
                .isPublished(true)
                .imageUrl("/assets/images/magazine-print.jpg")
                .specifications("Cover: 250gsm gloss, Inner: 120gsm matte, Binding: Saddle-stitch / Perfect bind")
                .build());

        Product rollupBanner = productRepository.save(Product.builder()
                .sku("BRD-BAN-ROLLUP-85")
                .barcode("600123450004")
                .name("Roll-up Pull Banner Stand (85cm x 200cm)")
                .slug("roll-up-pull-banner-stand-85x200")
                .description("Aluminum retractable roll-up banner stand with waterproof UV synthetic print and carry case.")
                .category(brandingCat)
                .baseUnit(uomPcs)
                .purchaseCost(BigDecimal.valueOf(1800.00))
                .sellingPrice(BigDecimal.valueOf(2650.00))
                .reorderThreshold(BigDecimal.valueOf(15))
                .status(ProductStatus.ACTIVE)
                .isQuotationOnly(false)
                .isPublished(true)
                .imageUrl("/assets/images/rollup-stand.jpg")
                .specifications("Graphic size: 850 x 2000 mm, Mechanism: Retractable spring cassette, Base: Aluminum")
                .build());

        // 8. Opening Inventory Balances & Movements
        StockBalance htvBalance = stockBalanceRepository.save(StockBalance.builder()
                .productId(htvWhite.getId())
                .warehouseId(hqWh.getId())
                .onHandQuantity(BigDecimal.valueOf(250))
                .reservedQuantity(BigDecimal.ZERO)
                .weightedAverageCost(BigDecimal.valueOf(180.0000))
                .build());

        stockMovementRepository.save(StockMovement.builder()
                .productId(htvWhite.getId())
                .warehouseId(hqWh.getId())
                .quantityDelta(BigDecimal.valueOf(250))
                .movementType(StockMovementType.OPENING_BALANCE)
                .referenceType(ReferenceType.MANUAL)
                .referenceId("INIT-SEED")
                .unitCost(BigDecimal.valueOf(180.0000))
                .totalCost(BigDecimal.valueOf(45000.00))
                .actor("SYSTEM")
                .notes("Initial system opening inventory")
                .build());

        StockBalance paperBalance = stockBalanceRepository.save(StockBalance.builder()
                .productId(offsetPaper.getId())
                .warehouseId(hqWh.getId())
                .onHandQuantity(BigDecimal.valueOf(120))
                .reservedQuantity(BigDecimal.ZERO)
                .weightedAverageCost(BigDecimal.valueOf(450.0000))
                .build());

        stockMovementRepository.save(StockMovement.builder()
                .productId(offsetPaper.getId())
                .warehouseId(hqWh.getId())
                .quantityDelta(BigDecimal.valueOf(120))
                .movementType(StockMovementType.OPENING_BALANCE)
                .referenceType(ReferenceType.MANUAL)
                .referenceId("INIT-SEED")
                .unitCost(BigDecimal.valueOf(450.0000))
                .totalCost(BigDecimal.valueOf(54000.00))
                .actor("SYSTEM")
                .notes("Initial system opening inventory")
                .build());

        StockBalance bannerBoleBalance = stockBalanceRepository.save(StockBalance.builder()
                .productId(rollupBanner.getId())
                .warehouseId(boleWh.getId())
                .onHandQuantity(BigDecimal.valueOf(35))
                .reservedQuantity(BigDecimal.ZERO)
                .weightedAverageCost(BigDecimal.valueOf(1800.0000))
                .build());

        stockMovementRepository.save(StockMovement.builder()
                .productId(rollupBanner.getId())
                .warehouseId(boleWh.getId())
                .quantityDelta(BigDecimal.valueOf(35))
                .movementType(StockMovementType.OPENING_BALANCE)
                .referenceType(ReferenceType.MANUAL)
                .referenceId("INIT-SEED")
                .unitCost(BigDecimal.valueOf(1800.0000))
                .totalCost(BigDecimal.valueOf(63000.00))
                .actor("SYSTEM")
                .notes("Initial system opening inventory")
                .build());

        log.info("Baseline data seeded successfully. Default admin account initialized (username: 'admin').");
    }

    private void seedServicesIfEmpty() {
        if (serviceOfferingRepository.count() > 0) {
            return;
        }

        log.info("Seeding corporate services into database service_offerings table...");

        serviceOfferingRepository.save(ServiceOffering.builder()
                .code("PRINT-PRESS")
                .title("Commercial Printing Press Services")
                .category("Printing Press")
                .description("High-volume offset, digital, and flexographic printing for books, magazines, packaging, stationery, calendars, and brochures.")
                .imageUrl("/assets/images/printing-press.jpg")
                .keyOfferings("Offset & Digital Printing; Book Binding & Finishing; Packaging Boxes & Labels; Brochures & Flyers")
                .isQuotationEnabled(true)
                .isActive(true)
                .displayOrder(1)
                .build());

        serviceOfferingRepository.save(ServiceOffering.builder()
                .code("BRAND-PROMO")
                .title("Corporate Branding & Promotion")
                .category("Branding & Promotion")
                .description("Complete promotional item branding, signage, corporate gifts, exhibition stands, banners, and vehicle branding.")
                .imageUrl("/assets/images/branding-promotion.jpg")
                .keyOfferings("Roll-up & Teardrop Banners; Corporate Gifts & Pens; Acrylic & 3D Signage; Vehicle Wrapping")
                .isQuotationEnabled(true)
                .isActive(true)
                .displayOrder(2)
                .build());

        serviceOfferingRepository.save(ServiceOffering.builder()
                .code("GARMENT-MAT")
                .title("Garment & Textile Materials Distribution")
                .category("Garment Materials")
                .description("Wholesale supply of garment fabrics, sewing threads, interlinings, heat transfer vinyl, buttons, and embroidery materials.")
                .imageUrl("/assets/images/garment-materials.jpg")
                .keyOfferings("Heat Transfer Vinyl & Foils; Premium Threads & Yarns; Garment Interlining & Fabrics; Buttons & Zippers")
                .isQuotationEnabled(false)
                .isActive(true)
                .displayOrder(3)
                .build());

        serviceOfferingRepository.save(ServiceOffering.builder()
                .code("PRINT-EQUIP")
                .title("Printing Materials & Equipment")
                .category("Equipment Distribution")
                .description("Professional printing consumables, plate making, UV inks, solvents, laminating films, and industrial spare parts.")
                .imageUrl("/assets/images/printing-equipment.jpg")
                .keyOfferings("Printing Plates & Chemicals; Eco-Solvent & UV Inks; Laminating Rolls & Films; Plotter & Press Spare Parts")
                .isQuotationEnabled(false)
                .isActive(true)
                .displayOrder(4)
                .build());

        log.info("Corporate services baseline seeded in database.");
    }
}

