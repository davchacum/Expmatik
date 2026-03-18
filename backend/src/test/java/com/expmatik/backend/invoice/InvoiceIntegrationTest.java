package com.expmatik.backend.invoice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.expmatik.backend.batch.DTOs.BatchCreate;
import com.expmatik.backend.invoice.DTOs.InvoiceRequest;
import com.expmatik.backend.invoice.DTOs.InvoiceRequestUpdate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import jakarta.transaction.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class InvoiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // == Pruebas para GET /api/invoices/{id} == //

    private static final Integer OLD_QUANTITY_PRODUCTINFO1 = 100;
    private static final BigDecimal OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO1 = new BigDecimal("1.5");

    private static final Integer OLD_QUANTITY_PRODUCTINFO2 = 200;
    private static final BigDecimal OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO2 = new BigDecimal("2.2");

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testGetInvoiceById() throws Exception {

        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(get("/api/invoices/"+ invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
                .andExpect(jsonPath("$.supplierName").value("Proveedor 1"))
                .andExpect(jsonPath("$.totalAmount").value(350));

    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testGetInvoiceById_NotFound() throws Exception {

        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000999");

        mockMvc.perform(get("/api/invoices/"+ invoiceId))
                .andExpect(status().isNotFound());

    }

    @Test
    @WithUserDetails("admin2@expmatik.com")
    void testGetInvoiceById_Unauthorized() throws Exception {

        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(get("/api/invoices/"+ invoiceId))
                .andExpect(status().isForbidden());

    }

    // == Pruebas para GET /api/invoices/search == //

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchInvoices_All() throws Exception {

        mockMvc.perform(get("/api/invoices/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));

    }

    @Test
    @WithUserDetails("admin2@expmatik.com")
    void testSearchInvoices_ZeroResults() throws Exception {

        mockMvc.perform(get("/api/invoices/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchInvoices_ByStatus() throws Exception {

        mockMvc.perform(get("/api/invoices/search")
                .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-001"));

    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchInvoices_ByStartDate() throws Exception {

        mockMvc.perform(get("/api/invoices/search")
                .param("startDate", "2024-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));

    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchInvoices_ByStartDate2() throws Exception {

        mockMvc.perform(get("/api/invoices/search")
                .param("startDate", "2024-01-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-002"));

    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchInvoices_ByEndDate() throws Exception {

        mockMvc.perform(get("/api/invoices/search")
                .param("endDate", "2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-001"));

    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchInvoices_ByEndDate2() throws Exception {

        mockMvc.perform(get("/api/invoices/search")
                .param("endDate", "2024-02-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));

    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchInvoices_ByStartDateAndEndDate() throws Exception {

        mockMvc.perform(get("/api/invoices/search")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-02-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));

    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchInvoices_ByStartDateAndEndDate2() throws Exception {

        mockMvc.perform(get("/api/invoices/search")
                .param("startDate", "2024-01-10")
                .param("endDate", "2024-02-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-002"));

    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchInvoices_BySupplier() throws Exception {

        mockMvc.perform(get("/api/invoices/search")
                .param("supplier", "Supplier 1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));

    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchInvoices_MinPrice() throws Exception {

        mockMvc.perform(get("/api/invoices/search")
                .param("minPrice", "79.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[1].totalAmount").value(79.5));

    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchInvoices_MinPrice2() throws Exception {

        mockMvc.perform(get("/api/invoices/search")
                .param("minPrice", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].totalAmount").value(350.0));

    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchInvoices_MaxPrice() throws Exception {

        mockMvc.perform(get("/api/invoices/search")
                .param("maxPrice", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].totalAmount").value(79.5));

    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchInvoices_MaxPrice2() throws Exception {

        mockMvc.perform(get("/api/invoices/search")
                .param("maxPrice", "350"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].totalAmount").value(350.0));

    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchInvoices_ByMinPriceAndMaxPrice() throws Exception {

        mockMvc.perform(get("/api/invoices/search")
                .param("endDate", "2024-01-15")
                .param("minPrice", "100")
                .param("maxPrice", "350"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-001"));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchInvoices_ByMinPriceAndMaxPrice2() throws Exception {

        mockMvc.perform(get("/api/invoices/search")
                .param("minPrice", "79.5")
                .param("maxPrice", "350"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchInvoices_ByInvoiceNumber() throws Exception {

        mockMvc.perform(get("/api/invoices/search")
                .param("invoiceNumber", "INV-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-001"));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchInvoices_ByInvoiceNumber2() throws Exception {

        mockMvc.perform(get("/api/invoices/search")
                .param("invoiceNumber", "INV"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testSearchInvoices_ByInvoiceNumber3() throws Exception {

        mockMvc.perform(get("/api/invoices/search")
                .param("invoiceNumber", "NADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // == Pruebas para GET /api/invoices/number/{invoiceNumber} == //

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testGetInvoiceByInvoiceNumber() throws Exception {

        mockMvc.perform(get("/api/invoices/number/INV-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
                .andExpect(jsonPath("$.supplierName").value("Proveedor 1"))
                .andExpect(jsonPath("$.totalAmount").value(350));

    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testGetInvoiceByInvoiceNumber_NotFound() throws Exception {

        mockMvc.perform(get("/api/invoices/number/INV-999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("admin2@expmatik.com")
    void testGetInvoiceByInvoiceNumber_Unauthorized() throws Exception {

        mockMvc.perform(get("/api/invoices/number/INV-001"))
                .andExpect(status().isForbidden());
    }

    // == Pruebas para POST /api/invoices == //


    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateInvoice() throws Exception {

        BatchCreate batchCreate = new BatchCreate(
            LocalDate.now().plusDays(10),
            new BigDecimal("5"),
            10,
            "20000001"
        );


        InvoiceRequest invoiceRequest = new InvoiceRequest(
            "INV-003",
            "Proveedor 1",
            InvoiceStatus.PENDING,
            new LinkedList<>(Arrays.asList(batchCreate)),
            LocalDate.now().plusDays(10)
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/invoices")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-003"))
                .andExpect(jsonPath("$.supplierName").value("Proveedor 1"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.invoiceDate").value(LocalDate.now().plusDays(10).toString()))
                .andExpect(jsonPath("$.totalAmount").value(50));

        //quantity + 10, lastPurchaseUnitPrice 5
        UUID productInfoId2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        mockMvc.perform(get("/api/product-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].productInfoId").value(productInfoId2.toString()))
                .andExpect(jsonPath("$[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2))
                .andExpect(jsonPath("$[0].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO2.doubleValue()));

        
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateInvoiceAndCreateSupplier() throws Exception {

        BatchCreate batchCreate = new BatchCreate(
            LocalDate.now().plusDays(10),
            new BigDecimal("5"),
            10,
            "20000001"
        );


        InvoiceRequest invoiceRequest = new InvoiceRequest(
            "INV-003",
            "Proveedor 2",
            InvoiceStatus.PENDING,
            new LinkedList<>(Arrays.asList(batchCreate)),
            LocalDate.now().plusDays(10)
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/invoices")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-003"))
                .andExpect(jsonPath("$.supplierName").value("Proveedor 2"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.invoiceDate").value(LocalDate.now().plusDays(10).toString()))
                .andExpect(jsonPath("$.totalAmount").value(50));

        //quantity + 10, lastPurchaseUnitPrice 5
        UUID productInfoId2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        mockMvc.perform(get("/api/product-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].productInfoId").value(productInfoId2.toString()))
                .andExpect(jsonPath("$[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2))
                .andExpect(jsonPath("$[0].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO2.doubleValue()));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateInvoiceAndCreateNonCustomProduct() throws Exception {

        BatchCreate batchCreate = new BatchCreate(
            LocalDate.now().plusDays(10),
            new BigDecimal("5"),
            10,
            "4716982022201"
        );

        InvoiceRequest invoiceRequest = new InvoiceRequest(
            "INV-003",
            "Proveedor 1",
            InvoiceStatus.PENDING,
            new LinkedList<>(Arrays.asList(batchCreate)),
            LocalDate.now().plusDays(10)
        );

        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.post("/api/invoices")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-003"))
                .andExpect(jsonPath("$.supplierName").value("Proveedor 1"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.invoiceDate").value(LocalDate.now().plusDays(10).toString()))
                .andExpect(jsonPath("$.totalAmount").value(50))
                .andExpect(jsonPath("$.batches[0].productName").value("Choco Bom"))
                .andReturn();

        String json = response.getResponse().getContentAsString();
        //Guardar uuid del product creado nuevo tras crear la factura
        String productId = JsonPath.read(json, "$.batches[0].productId");

        Integer prevQuantity2 = 0;
        //Obetner productInfoId del product creado nuevo tras crear la factura
        mockMvc.perform(get("/api/product-info/get-or-create-product/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.stockQuantity").value(prevQuantity2))
                .andExpect(jsonPath("$.lastPurchaseUnitPrice").doesNotExist());
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateInvoiceAndCreateNonCustomProduct_InvoiceStatusReceived() throws Exception {

        BatchCreate batchCreate = new BatchCreate(
            LocalDate.now().plusDays(10),
            new BigDecimal("5"),
            10,
            "4716982022201"
        );

        InvoiceRequest invoiceRequest = new InvoiceRequest(
            "INV-003",
            "Proveedor 1",
            InvoiceStatus.RECEIVED,
            new LinkedList<>(Arrays.asList(batchCreate)),
            LocalDate.now().plusDays(10)
        );

        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.post("/api/invoices")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-003"))
                .andExpect(jsonPath("$.supplierName").value("Proveedor 1"))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.invoiceDate").value(LocalDate.now().plusDays(10).toString()))
                .andExpect(jsonPath("$.totalAmount").value(50))
                .andExpect(jsonPath("$.batches[0].productName").value("Choco Bom"))
                .andReturn();

        String json = response.getResponse().getContentAsString();
        //Guardar uuid del product creado nuevo tras crear la factura
        String productId = JsonPath.read(json, "$.batches[0].productId");

        //quantity + 10, lastPurchaseUnitPrice 5
        Integer prevQuantity2 = 0;
        //Obetner productInfoId del product creado nuevo tras crear la factura
        mockMvc.perform(get("/api/product-info/get-or-create-product/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.stockQuantity").value(prevQuantity2+10))
                .andExpect(jsonPath("$.lastPurchaseUnitPrice").value(new BigDecimal("5").doubleValue()));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateInvoice_CanceledStatus() throws Exception {
        BatchCreate batchCreate = new BatchCreate(
            LocalDate.now().plusDays(10),
            new BigDecimal("5"),
            10,
            "20000001"
        );

        InvoiceRequest invoiceRequest = new InvoiceRequest(
            "INV-003",
            "Proveedor 1",
            InvoiceStatus.CANCELED,
            new LinkedList<>(Arrays.asList(batchCreate)),
            LocalDate.now().plusDays(10)
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/invoices")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-003"))
                .andExpect(jsonPath("$.supplierName").value("Proveedor 1"))
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.invoiceDate").value(LocalDate.now().plusDays(10).toString()))
                .andExpect(jsonPath("$.totalAmount").value(50));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateInvoice_ReceivedStatus() throws Exception {
        BatchCreate batchCreate = new BatchCreate(
            LocalDate.now().plusDays(10),
            new BigDecimal("5"),
            10,
            "20000001"
        );

        InvoiceRequest invoiceRequest = new InvoiceRequest(
            "INV-003",
            "Proveedor 1",
            InvoiceStatus.RECEIVED,
            new LinkedList<>(Arrays.asList(batchCreate)),
            LocalDate.now().plusDays(10)
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/invoices")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-003"))
                .andExpect(jsonPath("$.supplierName").value("Proveedor 1"))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.invoiceDate").value(LocalDate.now().plusDays(10).toString()))
                .andExpect(jsonPath("$.totalAmount").value(50));
        
        //quantity + 10, lastPurchaseUnitPrice 5
        UUID productInfoId2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        mockMvc.perform(get("/api/product-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].productInfoId").value(productInfoId2.toString()))
                .andExpect(jsonPath("$[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2 + 10))
                .andExpect(jsonPath("$[0].lastPurchaseUnitPrice").value(new BigDecimal("5").doubleValue()));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateInvoice_PerishableTrueAndExpirationDateNull() throws Exception {
        BatchCreate batchCreate = new BatchCreate(
            null,
            new BigDecimal("5"),
            10,
            "20000001"
        );

        InvoiceRequest invoiceRequest = new InvoiceRequest(
            "INV-003",
            "Proveedor 1",
            InvoiceStatus.PENDING,
            new LinkedList<>(Arrays.asList(batchCreate)),
            LocalDate.now().plusDays(10)
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/invoices")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateInvoice_PerishableFalseAndExpirationDateNotNull() throws Exception {
        BatchCreate batchCreate = new BatchCreate(
            LocalDate.now().plusDays(10),
            new BigDecimal("5"),
            10,
            "20000002"
        );

        InvoiceRequest invoiceRequest = new InvoiceRequest(
            "INV-003",
            "Proveedor 1",
            InvoiceStatus.PENDING,
            new LinkedList<>(Arrays.asList(batchCreate)),
            LocalDate.now().plusDays(10)
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/invoices")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isConflict());
    }
    

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateInvoice_InvoiceNumberAlreadyExists() throws Exception {
        BatchCreate batchCreate = new BatchCreate(
            LocalDate.now().plusDays(10),
            new BigDecimal("5"),
            10,
            "20000001"
        );

        InvoiceRequest invoiceRequest = new InvoiceRequest(
            "INV-001",
            "Proveedor 1",
            InvoiceStatus.PENDING,
            new LinkedList<>(Arrays.asList(batchCreate)),
            LocalDate.now().plusDays(10)
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/invoices")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isConflict());
    
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateInvoice_ProductNotFoundInOpenFoodFacts() throws Exception {
        BatchCreate batchCreate = new BatchCreate(
            LocalDate.now().plusDays(10),
            new BigDecimal("5"),
            10,
            "20000099"
        );

        InvoiceRequest invoiceRequest = new InvoiceRequest(
            "INV-003",
            "Proveedor 1",
            InvoiceStatus.PENDING,
            new LinkedList<>(Arrays.asList(batchCreate)),
            LocalDate.now().plusDays(10)
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/invoices")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("admin2@expmatik.com")
    void testCreateInvoice_ProductCustomExistsButDoesNotBelongToTheCurrentUser() throws Exception {
        BatchCreate batchCreate = new BatchCreate(
            LocalDate.now().plusDays(10),
            new BigDecimal("5"),
            10,
            "20000000"
        );

        InvoiceRequest invoiceRequest = new InvoiceRequest(
            "INV-003",
            "Proveedor 1",
            InvoiceStatus.PENDING,
            new LinkedList<>(Arrays.asList(batchCreate)),
            LocalDate.now().plusDays(10)
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/invoices")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateInvoice_EmptyBatchList() throws Exception {
        InvoiceRequest invoiceRequest = new InvoiceRequest(
            "INV-003",
            "Proveedor 1",
            InvoiceStatus.PENDING,
            new LinkedList<>(),
            LocalDate.now().plusDays(10)
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/invoices")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isBadRequest());
    }

    // == Pruebas para PUT /api/invoices/{id} == //

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testUpdateInvoice() throws Exception {

        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        InvoiceRequestUpdate invoiceRequest = new InvoiceRequestUpdate(
            "INV-001",
            "Proveedor 1",
            InvoiceStatus.PENDING,
            LocalDate.now().plusDays(10)
        );

        mockMvc.perform(put("/api/invoices/"+invoiceId)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
                .andExpect(jsonPath("$.supplierName").value("Proveedor 1"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.invoiceDate").value(LocalDate.now().plusDays(10).toString()))
                .andExpect(jsonPath("$.batches.length()").value(2))
                .andExpect(jsonPath("$.batches[0].productName").value("Leche Entera"))
                .andExpect(jsonPath("$.totalAmount").value(350));

        //quantity, lastPurchaseUnitPrice 2
        UUID productInfoId1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        //quantity, lastPurchaseUnitPrice 3
        UUID productInfoId2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        mockMvc.perform(get("/api/product-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].productInfoId").value(productInfoId2.toString()))
                .andExpect(jsonPath("$[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2))
                .andExpect(jsonPath("$[0].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO2))
                .andExpect(jsonPath("$[1].productInfoId").value(productInfoId1.toString()))
                .andExpect(jsonPath("$[1].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO1))
                .andExpect(jsonPath("$[1].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO1));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testUpdateInvoice_NewInvoiceNumber() throws Exception {

        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        InvoiceRequestUpdate invoiceRequest = new InvoiceRequestUpdate(
            "INV-003",
            "Proveedor 1",
            InvoiceStatus.PENDING,
            LocalDate.now().plusDays(10)
        );

        mockMvc.perform(put("/api/invoices/"+invoiceId)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-003"))
                .andExpect(jsonPath("$.supplierName").value("Proveedor 1"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.invoiceDate").value(LocalDate.now().plusDays(10).toString()))
                .andExpect(jsonPath("$.batches.length()").value(2))
                .andExpect(jsonPath("$.batches[0].productName").value("Leche Entera"))
                .andExpect(jsonPath("$.totalAmount").value(350));

        //quantity, lastPurchaseUnitPrice 2
        UUID productInfoId1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        //quantity, lastPurchaseUnitPrice 3
        UUID productInfoId2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        mockMvc.perform(get("/api/product-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].productInfoId").value(productInfoId2.toString()))
                .andExpect(jsonPath("$[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2))
                .andExpect(jsonPath("$[0].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO2))
                .andExpect(jsonPath("$[1].productInfoId").value(productInfoId1.toString()))
                .andExpect(jsonPath("$[1].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO1))
                .andExpect(jsonPath("$[1].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO1));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testUpdateInvoiceAndCreateSupplier() throws Exception {

        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        InvoiceRequestUpdate invoiceRequest = new InvoiceRequestUpdate(
            "INV-001",
            "Proveedor 99",
            InvoiceStatus.PENDING,
            LocalDate.now().plusDays(10)
        );

        mockMvc.perform(put("/api/invoices/"+invoiceId)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
                .andExpect(jsonPath("$.supplierName").value("Proveedor 99"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.invoiceDate").value(LocalDate.now().plusDays(10).toString()))
                .andExpect(jsonPath("$.batches.length()").value(2))
                .andExpect(jsonPath("$.batches[0].productName").value("Leche Entera"))
                .andExpect(jsonPath("$.totalAmount").value(350));
        
        //quantity, lastPurchaseUnitPrice 2
        UUID productInfoId1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        //quantity, lastPurchaseUnitPrice 3
        UUID productInfoId2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        mockMvc.perform(get("/api/product-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].productInfoId").value(productInfoId2.toString()))
                .andExpect(jsonPath("$[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2))
                .andExpect(jsonPath("$[0].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO2))
                .andExpect(jsonPath("$[1].productInfoId").value(productInfoId1.toString()))
                .andExpect(jsonPath("$[1].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO1))
                .andExpect(jsonPath("$[1].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO1));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testUpdateInvoice_CanceledStatus() throws Exception {
        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        InvoiceRequestUpdate invoiceRequest = new InvoiceRequestUpdate(
            "INV-001",
            "Proveedor 01",
            InvoiceStatus.CANCELED,
            LocalDate.now().plusDays(10)
        );

        mockMvc.perform(put("/api/invoices/"+invoiceId)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
                .andExpect(jsonPath("$.supplierName").value("Proveedor 01"))
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.invoiceDate").value(LocalDate.now().plusDays(10).toString()))
                .andExpect(jsonPath("$.batches.length()").value(2))
                .andExpect(jsonPath("$.batches[0].productName").value("Leche Entera"))
                .andExpect(jsonPath("$.totalAmount").value(350));

        //quantity, lastPurchaseUnitPrice 2
        UUID productInfoId1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        //quantity, lastPurchaseUnitPrice 3
        UUID productInfoId2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        mockMvc.perform(get("/api/product-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].productInfoId").value(productInfoId2.toString()))
                .andExpect(jsonPath("$[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2))
                .andExpect(jsonPath("$[0].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO2))
                .andExpect(jsonPath("$[1].productInfoId").value(productInfoId1.toString()))
                .andExpect(jsonPath("$[1].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO1))
                .andExpect(jsonPath("$[1].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO1));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testUpdateInvoice_ReceivedStatus() throws Exception {
        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        InvoiceRequestUpdate invoiceRequest = new InvoiceRequestUpdate(
            "INV-001",
            "Proveedor 01",
            InvoiceStatus.RECEIVED,
            LocalDate.now().plusDays(10)
        );

        mockMvc.perform(put("/api/invoices/"+invoiceId)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
                .andExpect(jsonPath("$.supplierName").value("Proveedor 01"))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.invoiceDate").value(LocalDate.now().plusDays(10).toString()))
                .andExpect(jsonPath("$.batches.length()").value(2))
                .andExpect(jsonPath("$.batches[0].productName").value("Leche Entera"))
                .andExpect(jsonPath("$.totalAmount").value(350));
        
        //quantity + 50, lastPurchaseUnitPrice 2
        UUID productInfoId1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        //quantity + 100, lastPurchaseUnitPrice 3
        UUID productInfoId2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        mockMvc.perform(get("/api/product-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].productInfoId").value(productInfoId2.toString()))
                .andExpect(jsonPath("$[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2 + 50))
                .andExpect(jsonPath("$[0].lastPurchaseUnitPrice").value(new BigDecimal("3").doubleValue()))
                .andExpect(jsonPath("$[1].productInfoId").value(productInfoId1.toString()))
                .andExpect(jsonPath("$[1].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO1 + 100))
                .andExpect(jsonPath("$[1].lastPurchaseUnitPrice").value(new BigDecimal("2").doubleValue()));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testUpdateInvoice_AndOldInvoiceNotPendingStatus() throws Exception {

        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        InvoiceRequestUpdate invoiceRequest = new InvoiceRequestUpdate(
            "INV-002",
            "Proveedor 01",
            InvoiceStatus.PENDING,
            LocalDate.now().plusDays(10)
        );

        mockMvc.perform(put("/api/invoices/"+invoiceId)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isConflict());
    }


    @Test
    @WithUserDetails("admin@expmatik.com")
    void testUpdateInvoice_InvoiceNumberAlreadyExists() throws Exception {

        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        InvoiceRequestUpdate invoiceRequest = new InvoiceRequestUpdate(
            "INV-002",
            "Proveedor 01",
            InvoiceStatus.PENDING,
            LocalDate.now().plusDays(10)
        );


        mockMvc.perform(put("/api/invoices/"+invoiceId)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isConflict());
    
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testUpdateInvoice_InvoiceNotFound() throws Exception {

        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000099");

        InvoiceRequestUpdate invoiceRequest = new InvoiceRequestUpdate(
            "INV-001",
            "Proveedor 01",
            InvoiceStatus.PENDING,
            LocalDate.now().plusDays(10)
        );

        mockMvc.perform(put("/api/invoices/"+invoiceId)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("admin2@expmatik.com")
    void testUpdateInvoice_InvoiceDoesNotBelongsCurrentUser() throws Exception {
        
        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        InvoiceRequestUpdate invoiceRequest = new InvoiceRequestUpdate(
            "INV-001",
            "Proveedor 01",
            InvoiceStatus.PENDING,
            LocalDate.now().plusDays(10)
        );

        mockMvc.perform(put("/api/invoices/"+invoiceId)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isForbidden());
    }

    // == Pruebas para DELETE /api/invoices/{id} == //

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testDeleteInvoice() throws Exception {

        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(delete("/api/invoices/"+invoiceId))
                .andExpect(status().isNoContent());

    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testDeleteInvoice_InvoiceNotFound() throws Exception {
        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000099");

        mockMvc.perform(delete("/api/invoices/"+invoiceId))
                .andExpect(status().isNotFound()); 
    }

    @Test
    @WithUserDetails("admin2@expmatik.com")
    void testDeleteInvoice_InvoiceDoesNotBelongCurrentUser() throws Exception {
        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(delete("/api/invoices/"+invoiceId))
                .andExpect(status().isForbidden()); 
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testDeleteInvoice_InvoiceNotPending() throws Exception {
        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        mockMvc.perform(delete("/api/invoices/"+invoiceId))
                .andExpect(status().isConflict()); 
    }

    // == Pruebas para PUT /api/invoices/{id}/status == //

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testUpdateInvoiceStatus() throws Exception {
        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        InvoiceStatus newStatus = InvoiceStatus.CANCELED;

        mockMvc.perform(patch("/api/invoices/"+invoiceId+"/status")
                .param("status", newStatus.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELED"));

        //quantity, lastPurchaseUnitPrice 2
        UUID productInfoId1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        //quantity, lastPurchaseUnitPrice 3
        UUID productInfoId2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        mockMvc.perform(get("/api/product-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].productInfoId").value(productInfoId2.toString()))
                .andExpect(jsonPath("$[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2))
                .andExpect(jsonPath("$[0].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO2))
                .andExpect(jsonPath("$[1].productInfoId").value(productInfoId1.toString()))
                .andExpect(jsonPath("$[1].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO1))
                .andExpect(jsonPath("$[1].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO1));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testUpdateInvoiceStatus2() throws Exception {
        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        InvoiceStatus newStatus = InvoiceStatus.RECEIVED;

        mockMvc.perform(patch("/api/invoices/"+invoiceId+"/status")
                .param("status", newStatus.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        //quantity + 50, lastPurchaseUnitPrice 2
        UUID productInfoId1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        //quantity + 100, lastPurchaseUnitPrice 3
        UUID productInfoId2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        mockMvc.perform(get("/api/product-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].productInfoId").value(productInfoId2.toString()))
                .andExpect(jsonPath("$[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2 + 50))
                .andExpect(jsonPath("$[0].lastPurchaseUnitPrice").value(new BigDecimal("3").doubleValue()))
                .andExpect(jsonPath("$[1].productInfoId").value(productInfoId1.toString()))
                .andExpect(jsonPath("$[1].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO1 + 100))
                .andExpect(jsonPath("$[1].lastPurchaseUnitPrice").value(new BigDecimal("2").doubleValue()));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testUpdateInvoiceStatus3() throws Exception {
        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        InvoiceStatus newStatus = InvoiceStatus.PENDING;

        mockMvc.perform(patch("/api/invoices/"+invoiceId+"/status")
                .param("status", newStatus.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));

        //quantity, lastPurchaseUnitPrice 2
        UUID productInfoId1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        //quantity, lastPurchaseUnitPrice 3
        UUID productInfoId2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        mockMvc.perform(get("/api/product-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].productInfoId").value(productInfoId2.toString()))
                .andExpect(jsonPath("$[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2))
                .andExpect(jsonPath("$[0].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO2))
                .andExpect(jsonPath("$[1].productInfoId").value(productInfoId1.toString()))
                .andExpect(jsonPath("$[1].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO1))
                .andExpect(jsonPath("$[1].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO1));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testUpdateInvoiceStatus_InvoiceNotFound() throws Exception {
        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        InvoiceStatus newStatus = InvoiceStatus.CANCELED;

        mockMvc.perform(patch("/api/invoices/"+invoiceId+"/status")
                .param("status", newStatus.name()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("admin2@expmatik.com")
    void testUpdateInvoiceStatus_InvoiceDoesNotBelongCurrentUser() throws Exception {
        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        InvoiceStatus newStatus = InvoiceStatus.CANCELED;

        mockMvc.perform(patch("/api/invoices/"+invoiceId+"/status")
                .param("status", newStatus.name()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testUpdateInvoiceStatus_OldNotPending() throws Exception {
        UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        InvoiceStatus newStatus = InvoiceStatus.CANCELED;

        mockMvc.perform(patch("/api/invoices/"+invoiceId+"/status")
                .param("status", newStatus.name()))
                .andExpect(status().isConflict());
    }

    // == Pruebas para POST /api/invoices/csv == //

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateInvoicesFromCSV() throws Exception {
        MockMultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
            ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
             "FAC-2026-001,Supplier A,PENDING,2026-03-10,20000001,24,0.85,2026-09-30\n" +
             "FAC-2026-001,Supplier A,PENDING,2026-03-10,20000001,10,1.20,2026-09-30\n" +
             "FAC-2026-001,Supplier A,PENDING,2026-03-10,20000000,10,1.20,\n" +
             "FAC-2026-002,Supplier B,PENDING,2026-03-11,20000001,24,1.20,2026-08-10\n" +
             "FAC-2026-002,Supplier B,PENDING,2026-03-11,20000001,12,0.95,2026-08-01").getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/invoices/csv")
                .file(csvContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].invoiceNumber").value("FAC-2026-001"))
                .andExpect(jsonPath("$[0].supplierName").value("Supplier A"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].invoiceDate").value("2026-03-10"))
                .andExpect(jsonPath("$[0].batches.length()").value(3))
                .andExpect(jsonPath("$[0].batches[0].quantity").value(24))
                .andExpect(jsonPath("$[0].batches[0].unitPrice").value(0.85))
                .andExpect(jsonPath("$[0].batches[0].expirationDate").value("2026-09-30"))
                .andExpect(jsonPath("$[0].batches[1].quantity").value(10))
                .andExpect(jsonPath("$[0].batches[1].unitPrice").value(1.20))
                .andExpect(jsonPath("$[0].batches[1].expirationDate").value("2026-09-30"))
                .andExpect(jsonPath("$[0].batches[2].quantity").value(10))
                .andExpect(jsonPath("$[0].batches[2].unitPrice").value(1.20))
                .andExpect(jsonPath("$[0].batches[2].expirationDate").doesNotExist())
                .andExpect(jsonPath("$[1].invoiceNumber").value("FAC-2026-002"))
                .andExpect(jsonPath("$[1].supplierName").value("Supplier B"))
                .andExpect(jsonPath("$[1].status").value("PENDING"))
                .andExpect(jsonPath("$[1].invoiceDate").value("2026-03-11"))
                .andExpect(jsonPath("$[1].batches.length()").value(2))
                .andExpect(jsonPath("$[1].batches[0].quantity").value(24))
                .andExpect(jsonPath("$[1].batches[0].unitPrice").value(1.20))
                .andExpect(jsonPath("$[1].batches[0].expirationDate").value("2026-08-10"))
                .andExpect(jsonPath("$[1].batches[1].quantity").value(12))
                .andExpect(jsonPath("$[1].batches[1].unitPrice").value(0.95))
                .andExpect(jsonPath("$[1].batches[1].expirationDate").value("2026-08-01"));

        //quantity, lastPurchaseUnitPrice 2
        UUID productInfoId1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        //quantity, lastPurchaseUnitPrice 3
        UUID productInfoId2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        mockMvc.perform(get("/api/product-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].productInfoId").value(productInfoId2.toString()))
                .andExpect(jsonPath("$[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2 ))
                .andExpect(jsonPath("$[0].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO2))
                .andExpect(jsonPath("$[1].productInfoId").value(productInfoId1.toString()))
                .andExpect(jsonPath("$[1].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO1))
                .andExpect(jsonPath("$[1].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO1));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateInvoicesFromCSVNotPending() throws Exception {
        MockMultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
            ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
             "FAC-2026-001,Supplier A,RECEIVED,2026-03-10,20000001,24,0.85,2026-09-30\n" +
             "FAC-2026-001,Supplier A,RECEIVED,2026-03-10,20000001,10,1.20,2026-09-30\n" +
             "FAC-2026-001,Supplier A,RECEIVED,2026-03-10,20000000,10,1.20,\n" +
             "FAC-2026-002,Supplier B,RECEIVED,2026-03-11,20000001,24,1.20,2026-08-10\n" +
             "FAC-2026-002,Supplier B,RECEIVED,2026-03-11,20000001,12,0.95,2026-08-01").getBytes());


        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/invoices/csv")
                .file(csvContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].invoiceNumber").value("FAC-2026-001"))
                .andExpect(jsonPath("$[0].supplierName").value("Supplier A"))
                .andExpect(jsonPath("$[0].status").value("RECEIVED"))
                .andExpect(jsonPath("$[0].invoiceDate").value("2026-03-10"))
                .andExpect(jsonPath("$[0].batches.length()").value(3))
                .andExpect(jsonPath("$[0].batches[0].quantity").value(24))
                .andExpect(jsonPath("$[0].batches[0].unitPrice").value(0.85))
                .andExpect(jsonPath("$[0].batches[0].expirationDate").value("2026-09-30"))
                .andExpect(jsonPath("$[0].batches[1].quantity").value(10))
                .andExpect(jsonPath("$[0].batches[1].unitPrice").value(1.20))
                .andExpect(jsonPath("$[0].batches[1].expirationDate").value("2026-09-30"))
                .andExpect(jsonPath("$[0].batches[2].quantity").value(10))
                .andExpect(jsonPath("$[0].batches[2].unitPrice").value(1.20))
                .andExpect(jsonPath("$[0].batches[2].expirationDate").doesNotExist())
                .andExpect(jsonPath("$[1].invoiceNumber").value("FAC-2026-002"))
                .andExpect(jsonPath("$[1].supplierName").value("Supplier B"))
                .andExpect(jsonPath("$[1].status").value("RECEIVED"))
                .andExpect(jsonPath("$[1].invoiceDate").value("2026-03-11"))
                .andExpect(jsonPath("$[1].batches.length()").value(2))
                .andExpect(jsonPath("$[1].batches[0].quantity").value(24))
                .andExpect(jsonPath("$[1].batches[0].unitPrice").value(1.20))
                .andExpect(jsonPath("$[1].batches[0].expirationDate").value("2026-08-10"))
                .andExpect(jsonPath("$[1].batches[1].quantity").value(12))
                .andExpect(jsonPath("$[1].batches[1].unitPrice").value(0.95))
                .andExpect(jsonPath("$[1].batches[1].expirationDate").value("2026-08-01"));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateInvoicesFromCSVWithNonCustomProductNotInDatabase() throws Exception {
        MockMultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
            ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
             "FAC-2026-001,Supplier B,PENDING,2026-03-11,4716982022201,12,0.95,2026-08-01").getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/invoices/csv")
                .file(csvContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].invoiceNumber").value("FAC-2026-001"))
                .andExpect(jsonPath("$[0].supplierName").value("Supplier B"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].invoiceDate").value("2026-03-11"))
                .andExpect(jsonPath("$[0].batches.length()").value(1))
                .andExpect(jsonPath("$[0].batches[0].quantity").value(12))
                .andExpect(jsonPath("$[0].batches[0].unitPrice").value(0.95))
                .andExpect(jsonPath("$[0].batches[0].expirationDate").value("2026-08-01"));

    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateInvoicesFromCSV_SupplierMismatch() throws Exception {
        MockMultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
            ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
            "FAC-2026-001,Supplier A,PENDING,2026-03-11,20000000,12,0.95,\n" +
            "FAC-2026-001,Supplier B,PENDING,2026-03-11,20000000,12,0.95,").getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/invoices/csv")
                .file(csvContent))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateInvoicesFromCSV_StatusMismatch() throws Exception {
        MockMultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
            ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
            "FAC-2026-001,Supplier B,RECEIVED,2026-03-11,20000000,12,0.95,\n" +
            "FAC-2026-001,Supplier B,PENDING,2026-03-11,20000000,12,0.95,").getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/invoices/csv")
                .file(csvContent))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateInvoicesFromCSV_DateMismatch() throws Exception {
        MockMultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
            ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
            "FAC-2026-001,Supplier B,PENDING,2026-03-12,20000000,12,0.95,\n" +
            "FAC-2026-001,Supplier B,PENDING,2026-03-11,20000000,12,0.95,").getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/invoices/csv")
                .file(csvContent))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("admin2@expmatik.com")
    void testCreateInvoicesFromCSV_EmptyFile() throws Exception {

        MockMultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
            ("").getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/invoices/csv")
                .file(csvContent))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("admin2@expmatik.com")
    void testCreateInvoicesFromCSV_NoCsvFileExtension() throws Exception {
        MockMultipartFile csvContent = new MockMultipartFile("file", "invoices.txt", "text/csv",
            ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
            "FAC-2026-001,Supplier B,PENDING,2026-03-11,20000000,12,0.95,").getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/invoices/csv")
                .file(csvContent))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateInvoicesFromCSV_NoHeaders() throws Exception {
        MockMultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
            ("FAC-2026-001,Supplier B,PENDING,2026-03-11,20000000,12,0.95,").getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/invoices/csv")
                .file(csvContent))
                .andExpect(status().isOk());
    }

}

