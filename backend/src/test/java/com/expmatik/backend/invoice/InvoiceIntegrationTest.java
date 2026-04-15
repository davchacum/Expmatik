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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

    private static final Integer OLD_QUANTITY_PRODUCTINFO1 = 100;
    private static final BigDecimal OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO1 = new BigDecimal("1.5");

    private static final Integer OLD_QUANTITY_PRODUCTINFO2 = 200;
    private static final BigDecimal OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO2 = new BigDecimal("2.2");

    private static final Integer OLD_QUANTITY_PRODUCTINFO3 = 150;
    private static final BigDecimal OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO3 = new BigDecimal("1.0");

    // == Pruebas para GET /api/invoices/{id} == //

    @Nested
    @DisplayName("GET /api/invoices/{id}")
    class GetInvoiceById {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("GET /api/invoices/{id} - Valid ID and user is owner should return invoice details")
            @WithUserDetails("admin@expmatik.com")
            void testGetInvoiceById_ValidIdAndUser_success() throws Exception {

                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(get("/api/invoices/"+ invoiceId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                        .andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
                        .andExpect(jsonPath("$.supplierName").value("Proveedor 1"))
                        .andExpect(jsonPath("$.totalAmount").value(350));

            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("GET /api/invoices/{id} - Invalid ID should return Not Found")
            void testGetInvoiceById_InvalidId_shouldReturnNotFound() throws Exception {

                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000999");

                mockMvc.perform(get("/api/invoices/"+ invoiceId))
                        .andExpect(status().isNotFound());

            }

            @Test
            @WithUserDetails("admin2@expmatik.com")
            @DisplayName("GET /api/invoices/{id} - Invoice exists but user is not the owner should return Forbidden")
            void testGetInvoiceById_InvoiceNotOwnedByUser_shouldReturnForbidden() throws Exception {

                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(get("/api/invoices/"+ invoiceId))
                        .andExpect(status().isForbidden());

            }

            @Test
            @WithUserDetails("repo@expmatik.com")
            @DisplayName("GET /api/invoices/{id} - Unauthorized user should return Forbidden")
            void testGetInvoiceById_UnauthorizedUser_shouldReturnForbidden() throws Exception {

                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(get("/api/invoices/"+ invoiceId))
                        .andExpect(status().isForbidden());
            }
        }
    }

    // == Pruebas para GET /api/invoices/search == //

    @Nested
    @DisplayName("GET /api/invoices/search")
    class SearchInvoices {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("GET /api/invoices/search - Search all invoices without filters should return all user's invoices")
            void testSearchInvoices_SearchAll_success() throws Exception {

                mockMvc.perform(get("/api/invoices/search"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(2))
                        .andExpect(jsonPath("$.totalElements").value(2));

            }

            @Test
            @WithUserDetails("admin2@expmatik.com")
            @DisplayName("GET /api/invoices/search - Search all invoices without filters but user has no invoices should return empty result")
            void testSearchInvoices_ZeroResults_success() throws Exception {

                mockMvc.perform(get("/api/invoices/search"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(0))
                        .andExpect(jsonPath("$.totalElements").value(0));

            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("GET /api/invoices/search - Search by status should return filtered results")
            void testSearchInvoices_ByStatus_success() throws Exception {

                mockMvc.perform(get("/api/invoices/search")
                        .param("status", "PENDING"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(1))
                        .andExpect(jsonPath("$.totalElements").value(1))
                        .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-001"));

            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("GET /api/invoices/search - Search by status should return empty result if no invoices match the status")
            void testSearchInvoices_ByStartDate_success() throws Exception {

                mockMvc.perform(get("/api/invoices/search")
                        .param("startDate", "2024-01-01"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(2))
                        .andExpect(jsonPath("$.totalElements").value(2));

            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("GET /api/invoices/search - Search by start date should return filtered results")
            void testSearchInvoices_ByStartDate2_success() throws Exception {

                mockMvc.perform(get("/api/invoices/search")
                        .param("startDate", "2024-01-10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(1))
                        .andExpect(jsonPath("$.totalElements").value(1))
                        .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-002"));

            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("GET /api/invoices/search - Search by end date should return filtered results")
            void testSearchInvoices_ByEndDate_success() throws Exception {

                mockMvc.perform(get("/api/invoices/search")
                        .param("endDate", "2024-01-15"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(1))
                        .andExpect(jsonPath("$.totalElements").value(1))
                        .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-001"));

            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("GET /api/invoices/search - Search by end date should return empty result if no invoices match the end date")
            void testSearchInvoices_ByEndDate2_success() throws Exception {

                mockMvc.perform(get("/api/invoices/search")
                        .param("endDate", "2024-02-02"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(2))
                        .andExpect(jsonPath("$.totalElements").value(2));

            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("GET /api/invoices/search - Search by start date and end date should return filtered results")
            void testSearchInvoices_ByStartDateAndEndDate_success() throws Exception {

                mockMvc.perform(get("/api/invoices/search")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-02-02"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(2))
                        .andExpect(jsonPath("$.totalElements").value(2));

            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("GET /api/invoices/search - Search by start date and end date should return filtered results")
            void testSearchInvoices_ByStartDateAndEndDate2_success() throws Exception {

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
            @DisplayName("GET /api/invoices/search - Search by supplier should return filtered results")
            void testSearchInvoices_BySupplier_success() throws Exception {

                mockMvc.perform(get("/api/invoices/search")
                        .param("supplier", "Supplier 1"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(2))
                        .andExpect(jsonPath("$.totalElements").value(2));

            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("GET /api/invoices/search - Search by supplier should return empty result if no invoices match the supplier")
            void testSearchInvoices_MinPrice_success() throws Exception {

                mockMvc.perform(get("/api/invoices/search")
                        .param("minPrice", "79.5"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(2))
                        .andExpect(jsonPath("$.totalElements").value(2))
                        .andExpect(jsonPath("$.content[1].totalAmount").value(79.5));

            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("GET /api/invoices/search - Search by min price should return filtered results")
            void testSearchInvoices_MinPrice2_success() throws Exception {

                mockMvc.perform(get("/api/invoices/search")
                        .param("minPrice", "100"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(1))
                        .andExpect(jsonPath("$.totalElements").value(1))
                        .andExpect(jsonPath("$.content[0].totalAmount").value(350.0));

            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("GET /api/invoices/search - Search by max price should return filtered results")
            void testSearchInvoices_MaxPrice_success() throws Exception {

                mockMvc.perform(get("/api/invoices/search")
                        .param("maxPrice", "100"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(1))
                        .andExpect(jsonPath("$.totalElements").value(1))
                        .andExpect(jsonPath("$.content[0].totalAmount").value(79.5));

            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("GET /api/invoices/search - Search by max price should return empty result if no invoices match the max price")
            void testSearchInvoices_MaxPrice2_success() throws Exception {

                mockMvc.perform(get("/api/invoices/search")
                        .param("maxPrice", "350"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(2))
                        .andExpect(jsonPath("$.totalElements").value(2))
                        .andExpect(jsonPath("$.content[0].totalAmount").value(350.0));

            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("GET /api/invoices/search - Search by min price and max price should return filtered results")
            void testSearchInvoices_ByMinPriceAndMaxPrice_success() throws Exception {

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
            @DisplayName("GET /api/invoices/search - Search by min price and max price should return empty result if no invoices match the min price and max price")
            void testSearchInvoices_ByMinPriceAndMaxPrice2_success() throws Exception {

                mockMvc.perform(get("/api/invoices/search")
                        .param("minPrice", "79.5")
                        .param("maxPrice", "350"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(2))
                        .andExpect(jsonPath("$.totalElements").value(2));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("GET /api/invoices/search - Search by invoice number should return filtered results")
            void testSearchInvoices_ByInvoiceNumber_success() throws Exception {

                mockMvc.perform(get("/api/invoices/search")
                        .param("invoiceNumber", "INV-001"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(1))
                        .andExpect(jsonPath("$.totalElements").value(1))
                        .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-001"));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("GET /api/invoices/search - Search by invoice number with partial match should return filtered results")
            void testSearchInvoices_ByInvoiceNumber2_success() throws Exception {

                mockMvc.perform(get("/api/invoices/search")
                        .param("invoiceNumber", "INV"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(2))
                        .andExpect(jsonPath("$.totalElements").value(2));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("GET /api/invoices/search - Search by invoice number should return empty result if no invoices match the invoice number")
            void testSearchInvoices_ByInvoiceNumber3_success() throws Exception {

                mockMvc.perform(get("/api/invoices/search")
                        .param("invoiceNumber", "NADA"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(0))
                        .andExpect(jsonPath("$.totalElements").value(0));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {
            
            @Test
            @WithUserDetails("repo@expmatik.com")
            @DisplayName("GET /api/invoices/search - Unauthorized user should return Forbidden")
            void testSearchInvoices_UnauthorizedUser_shouldReturnForbidden() throws Exception {

                mockMvc.perform(get("/api/invoices/search"))
                        .andExpect(status().isForbidden());
            }
        }
    }

    // == Pruebas para GET /api/invoices/number/{invoiceNumber} == //

    @Nested
    @DisplayName("GET /api/invoices/number/{invoiceNumber}")
    class GetInvoiceByInvoiceNumber {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("GET /api/invoices/number/{invoiceNumber} - Valid invoice number and user is owner should return invoice details")
            void testGetInvoiceByInvoiceNumber() throws Exception {

                mockMvc.perform(get("/api/invoices/number/INV-001"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
                        .andExpect(jsonPath("$.supplierName").value("Proveedor 1"))
                        .andExpect(jsonPath("$.totalAmount").value(350));

            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("GET /api/invoices/number/{invoiceNumber} - Invalid invoice number should return Not Found")
            void testGetInvoiceByInvoiceNumber_InvalidInvoiceNumber_shouldReturnNotFound() throws Exception {

                mockMvc.perform(get("/api/invoices/number/INV-999"))
                        .andExpect(status().isNotFound());
            }

            @Test
            @WithUserDetails("admin2@expmatik.com")
            @DisplayName("GET /api/invoices/number/{invoiceNumber} - Invoice exists but user is not the owner should return Forbidden")
            void testGetInvoiceByInvoiceNumber_InvoiceNotOwnedByUser_shouldReturnForbidden() throws Exception {

                mockMvc.perform(get("/api/invoices/number/INV-001"))
                        .andExpect(status().isForbidden());
            }

            @Test
            @WithUserDetails("repo@expmatik.com")
            @DisplayName("GET /api/invoices/number/{invoiceNumber} - Unauthorized user should return Forbidden")
            void testGetInvoiceByInvoiceNumber_UnauthorizedUser_shouldReturnForbidden() throws Exception {

                mockMvc.perform(get("/api/invoices/number/INV-001"))
                        .andExpect(status().isForbidden());
            }
        }
    }

    // == Pruebas para POST /api/invoices == //

    @Nested
    @DisplayName("POST /api/invoices")
    class CreateInvoice {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("POST /api/invoices - Valid data should return created invoice")
            void testCreateInvoice_ValidData_shouldReturnCreatedInvoice() throws Exception {

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
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content[0].productInfoId").value(productInfoId2.toString()))
                        .andExpect(jsonPath("$.content[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2))
                        .andExpect(jsonPath("$.content[0].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO2.doubleValue()));

                
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("POST /api/invoices - Create supplier should return created invoice")
            void testCreateInvoice_CreateSupplier_shouldReturnCreatedInvoice() throws Exception {

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
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content[0].productInfoId").value(productInfoId2.toString()))
                        .andExpect(jsonPath("$.content[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2))
                        .andExpect(jsonPath("$.content[0].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO2.doubleValue()));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("POST /api/invoices - Create non-custom product should return created invoice")
            void testCreateInvoice_CreateNonCustomProduct_shouldReturnCreatedInvoice() throws Exception {
                Thread.sleep(2000);
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
            @DisplayName("POST /api/invoices - Create non-custom product and set invoice status to RECEIVED should return created invoice")
            void testCreateInvoice_CreateNonCustomProductAndInvoiceStatusReceived_shouldReturnCreatedInvoice() throws Exception {
                Thread.sleep(5000);
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
            @DisplayName("POST /api/invoices - Create invoice with CANCELED status should return created invoice without updating stock or last purchase unit price")
            void testCreateInvoice_CanceledStatus_shouldReturnCreatedInvoice() throws Exception {
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
            @DisplayName("POST /api/invoices - Create invoice with RECEIVED status should return created invoice and update stock and last purchase unit price")
            void testCreateInvoice_ReceivedStatus_shouldReturnCreatedInvoice() throws Exception {
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
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content[0].productInfoId").value(productInfoId2.toString()))
                        .andExpect(jsonPath("$.content[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2 + 10))
                        .andExpect(jsonPath("$.content[0].lastPurchaseUnitPrice").value(new BigDecimal("5").doubleValue()));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("POST /api/invoices - Perishable true and expiration date null should return Conflict")
            void testCreateInvoice_PerishableTrueAndExpirationDateNull_shouldReturnConflict() throws Exception {
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
            @DisplayName("POST /api/invoices - Perishable false and expiration date not null should return Conflict")
            void testCreateInvoice_PerishableFalseAndExpirationDateNotNull_shouldReturnConflict() throws Exception {
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
            @DisplayName("POST /api/invoices - Invoice number already exists should return Conflict")
            void testCreateInvoice_InvoiceNumberAlreadyExists_shouldReturnConflict() throws Exception {
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
            @DisplayName("POST /api/invoices - Product not found in Open Food Facts should return Not Found")
            void testCreateInvoice_ProductNotFoundInOpenFoodFacts_shouldReturnNotFound() throws Exception {
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
            @DisplayName("POST /api/invoices - Product custom exists but does not belong to the current user should return Not Found")
            void testCreateInvoice_ProductCustomExistsButDoesNotBelongToTheCurrentUser_shouldReturnNotFound() throws Exception {
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
            @DisplayName("POST /api/invoices - Empty batch list should return Bad Request")
            void testCreateInvoice_EmptyBatchList_shouldReturnBadRequest() throws Exception {
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

            @Test
            @WithUserDetails("repo@expmatik.com")
            @DisplayName("POST /api/invoices - Unauthorized user should return Forbidden")
            void testCreateInvoice_UnauthorizedUser_shouldReturnForbidden() throws Exception {
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
                        .andExpect(status().isForbidden());
            }
        }
    }

    // == Pruebas para PUT /api/invoices/{id} == //

    @Nested
    @DisplayName("PUT /api/invoices/{id}")
    class UpdateInvoice {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("PUT /api/invoices/{id} - Valid data and existing invoice should update invoice")
            void testUpdateInvoice_ValidDataAndExistingInvoice_shouldUpdateInvoice() throws Exception {

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

                UUID productInfoId3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

                mockMvc.perform(get("/api/product-info"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content[0].productInfoId").value(productInfoId2.toString()))
                        .andExpect(jsonPath("$.content[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2))
                        .andExpect(jsonPath("$.content[0].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO2))
                        .andExpect(jsonPath("$.content[1].productInfoId").value(productInfoId3.toString()))
                        .andExpect(jsonPath("$.content[1].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO3))
                        .andExpect(jsonPath("$.content[1].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO3))
                        .andExpect(jsonPath("$.content[2].productInfoId").value(productInfoId1.toString()))
                        .andExpect(jsonPath("$.content[2].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO1))
                        .andExpect(jsonPath("$.content[2].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO1));

            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("PUT /api/invoices/{id} - Update invoice number to a new unique invoice number should update invoice")
            void testUpdateInvoice_NewInvoiceNumber_ShouldUpdateInvoice() throws Exception {

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

                UUID productInfoId3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

                mockMvc.perform(get("/api/product-info"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content[0].productInfoId").value(productInfoId2.toString()))
                        .andExpect(jsonPath("$.content[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2))
                        .andExpect(jsonPath("$.content[0].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO2))
                        .andExpect(jsonPath("$.content[1].productInfoId").value(productInfoId3.toString()))
                        .andExpect(jsonPath("$.content[1].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO3))
                        .andExpect(jsonPath("$.content[1].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO3))
                        .andExpect(jsonPath("$.content[2].productInfoId").value(productInfoId1.toString()))
                        .andExpect(jsonPath("$.content[2].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO1))
                        .andExpect(jsonPath("$.content[2].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO1));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("PUT /api/invoices/{id} - Update supplier to a new supplier should update invoice")
            void testUpdateInvoice_CreateSupplier_shouldUpdateInvoice() throws Exception {

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

                UUID productInfoId3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

                mockMvc.perform(get("/api/product-info"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content[0].productInfoId").value(productInfoId2.toString()))
                        .andExpect(jsonPath("$.content[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2))
                        .andExpect(jsonPath("$.content[0].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO2))
                        .andExpect(jsonPath("$.content[1].productInfoId").value(productInfoId3.toString()))
                        .andExpect(jsonPath("$.content[1].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO3))
                        .andExpect(jsonPath("$.content[1].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO3))
                        .andExpect(jsonPath("$.content[2].productInfoId").value(productInfoId1.toString()))
                        .andExpect(jsonPath("$.content[2].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO1))
                        .andExpect(jsonPath("$.content[2].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO1));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("PUT /api/invoices/{id} - Update invoice status to CANCELED should update invoice without updating stock or last purchase unit price")
            void testUpdateInvoice_CanceledStatus_ShouldUpdateInvoice() throws Exception {
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

                UUID productInfoId3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

                mockMvc.perform(get("/api/product-info"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content[0].productInfoId").value(productInfoId2.toString()))
                        .andExpect(jsonPath("$.content[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2))
                        .andExpect(jsonPath("$.content[0].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO2))
                        .andExpect(jsonPath("$.content[1].productInfoId").value(productInfoId3.toString()))
                        .andExpect(jsonPath("$.content[1].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO3))
                        .andExpect(jsonPath("$.content[1].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO3))
                        .andExpect(jsonPath("$.content[2].productInfoId").value(productInfoId1.toString()))
                        .andExpect(jsonPath("$.content[2].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO1))
                        .andExpect(jsonPath("$.content[2].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO1));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("PUT /api/invoices/{id} - Update invoice status to RECEIVED should update invoice and update stock and last purchase unit price")
            void testUpdateInvoice_ReceivedStatus_ShouldUpdateInvoice() throws Exception {
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
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content[0].productInfoId").value(productInfoId2.toString()))
                        .andExpect(jsonPath("$.content[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2 + 50))
                        .andExpect(jsonPath("$.content[0].lastPurchaseUnitPrice").value(new BigDecimal("3").doubleValue()))
                        .andExpect(jsonPath("$.content[1].productInfoId").value(productInfoId1.toString()))
                        .andExpect(jsonPath("$.content[1].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO1 + 100))
                        .andExpect(jsonPath("$.content[1].lastPurchaseUnitPrice").value(new BigDecimal("2").doubleValue()));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("PUT /api/invoices/{id} - Update invoice status to RECEIVED when old invoice status is not PENDING should update invoice and update stock and last purchase unit price")
            void testUpdateInvoice_AndOldInvoiceNotPendingStatus_ShouldReturnConflict() throws Exception {

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
            @DisplayName("PUT /api/invoices/{id} - Update invoice number to an existing invoice number should return Conflict")
            void testUpdateInvoice_InvoiceNumberAlreadyExists_ShouldReturnConflict() throws Exception {

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
            @DisplayName("PUT /api/invoices/{id} - Update non existing invoice should return Not Found")
            void testUpdateInvoice_InvoiceNotFound_ShouldReturnNotFound() throws Exception {

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
            @DisplayName("PUT /api/invoices/{id} - Update invoice that does not belong to the current user should return Forbidden")
            void testUpdateInvoice_InvoiceDoesNotBelongsCurrentUser_ShouldReturnForbidden() throws Exception {
                
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

            @Test
            @WithUserDetails("repo@expmatik.com")
            @DisplayName("PUT /api/invoices/{id} - Unauthorized user should return Forbidden")
            void testUpdateInvoice_UnauthorizedUser_ShouldReturnForbidden() throws Exception {
                
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
        }
    }

    // == Pruebas para DELETE /api/invoices/{id} == //

    @Nested
    @DisplayName("DELETE /api/invoices/{id}")
    class DeleteInvoice {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("DELETE /api/invoices/{id} - Valid invoice should return No Content and delete the invoice")
            void testDeleteInvoice_ValidInvoice_ShouldReturnNoContent() throws Exception {

                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(delete("/api/invoices/"+invoiceId))
                        .andExpect(status().isNoContent());

            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("DELETE /api/invoices/{id} - Invalid invoice should return Not Found")
            void testDeleteInvoice_InvalidInvoice_ShouldReturnNotFound() throws Exception {
                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000099");

                mockMvc.perform(delete("/api/invoices/"+invoiceId))
                        .andExpect(status().isNotFound()); 
            }

            @Test
            @WithUserDetails("admin2@expmatik.com")
            @DisplayName("DELETE /api/invoices/{id} - Invoice that does not belong to the current user should return Forbidden")
            void testDeleteInvoice_InvoiceDoesNotBelongCurrentUser_ShouldReturnForbidden() throws Exception {
                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(delete("/api/invoices/"+invoiceId))
                        .andExpect(status().isForbidden()); 
            }

            @Test
            @WithUserDetails("repo@expmatik.com")
            @DisplayName("DELETE /api/invoices/{id} - Unauthorized user should return Forbidden")
            void testDeleteInvoice_UnauthorizedUser_ShouldReturnForbidden() throws Exception {
                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(delete("/api/invoices/"+invoiceId))
                        .andExpect(status().isForbidden()); 
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("DELETE /api/invoices/{id} - Invoice with status different than PENDING should return Conflict")
            void testDeleteInvoice_InvoiceNotPending_ShouldReturnConflict() throws Exception {
                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000002");

                mockMvc.perform(delete("/api/invoices/"+invoiceId))
                        .andExpect(status().isConflict()); 
            }
        }
    }

    // == Pruebas para PUT /api/invoices/{id}/status == //

    @Nested
    @DisplayName("PATCH /api/invoices/{id}/status")
    class UpdateInvoiceStatus {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("PATCH /api/invoices/{id}/status - Update invoice status to CANCELED should update status without updating stock or last purchase unit price")
            void testUpdateInvoiceStatus_ValidInvoiceCanceled_ShouldUpdateStatus() throws Exception {
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

                UUID productInfoId3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

                mockMvc.perform(get("/api/product-info"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content[0].productInfoId").value(productInfoId2.toString()))
                        .andExpect(jsonPath("$.content[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2))
                        .andExpect(jsonPath("$.content[0].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO2))
                        .andExpect(jsonPath("$.content[1].productInfoId").value(productInfoId3.toString()))
                        .andExpect(jsonPath("$.content[1].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO3))
                        .andExpect(jsonPath("$.content[1].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO3))
                        .andExpect(jsonPath("$.content[2].productInfoId").value(productInfoId1.toString()))
                        .andExpect(jsonPath("$.content[2].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO1))
                        .andExpect(jsonPath("$.content[2].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO1));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("PATCH /api/invoices/{id}/status - Update invoice status to RECEIVED should update status and update stock and last purchase unit price")
            void testUpdateInvoiceStatus_ValidInvoiceReceived_ShouldUpdateStatus() throws Exception {
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

                UUID productInfoId3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

                mockMvc.perform(get("/api/product-info"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content[0].productInfoId").value(productInfoId2.toString()))
                        .andExpect(jsonPath("$.content[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2 + 50))
                        .andExpect(jsonPath("$.content[0].lastPurchaseUnitPrice").value(new BigDecimal("3").doubleValue()))
                        .andExpect(jsonPath("$.content[1].productInfoId").value(productInfoId1.toString()))
                        .andExpect(jsonPath("$.content[1].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO1 + 100))
                        .andExpect(jsonPath("$.content[1].lastPurchaseUnitPrice").value(new BigDecimal("2").doubleValue()))
                        .andExpect(jsonPath("$.content[2].productInfoId").value(productInfoId3.toString()))
                        .andExpect(jsonPath("$.content[2].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO3))
                        .andExpect(jsonPath("$.content[2].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO3));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("PATCH /api/invoices/{id}/status - Update invoice status to PENDING should update status without updating stock or last purchase unit price")
            void testUpdateInvoiceStatus_ValidInvoicePending_ShouldUpdateStatus() throws Exception {
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

                UUID productInfoId3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

                mockMvc.perform(get("/api/product-info"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content[0].productInfoId").value(productInfoId2.toString()))
                        .andExpect(jsonPath("$.content[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2))
                        .andExpect(jsonPath("$.content[0].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO2))
                        .andExpect(jsonPath("$.content[1].productInfoId").value(productInfoId3.toString()))
                        .andExpect(jsonPath("$.content[1].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO3))
                        .andExpect(jsonPath("$.content[1].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO3))
                        .andExpect(jsonPath("$.content[2].productInfoId").value(productInfoId1.toString()))
                        .andExpect(jsonPath("$.content[2].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO1))
                        .andExpect(jsonPath("$.content[2].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO1));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("PATCH /api/invoices/{id}/status - Invalid invoice should return Not Found")
            void testUpdateInvoiceStatus_InvalidInvoiceId_shouldReturnNotFound() throws Exception {
                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000099");
                InvoiceStatus newStatus = InvoiceStatus.CANCELED;

                mockMvc.perform(patch("/api/invoices/"+invoiceId+"/status")
                        .param("status", newStatus.name()))
                        .andExpect(status().isNotFound());
            }

            @Test
            @WithUserDetails("admin2@expmatik.com")
            @DisplayName("PATCH /api/invoices/{id}/status - Invoice that does not belong to the current user should return Forbidden")
            void testUpdateInvoiceStatus_InvoiceDoesNotBelongCurrentUser_shouldReturnForbidden() throws Exception {
                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
                InvoiceStatus newStatus = InvoiceStatus.CANCELED;

                mockMvc.perform(patch("/api/invoices/"+invoiceId+"/status")
                        .param("status", newStatus.name()))
                        .andExpect(status().isForbidden());
            }

            @Test
            @WithUserDetails("repo@expmatik.com")
            @DisplayName("PATCH /api/invoices/{id}/status - Unauthorized user should return Forbidden")
            void testUpdateInvoiceStatus_UnauthorizedUser_shouldReturnForbidden() throws Exception {
                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
                InvoiceStatus newStatus = InvoiceStatus.CANCELED;

                mockMvc.perform(patch("/api/invoices/"+invoiceId+"/status")
                        .param("status", newStatus.name()))
                        .andExpect(status().isForbidden());
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("PATCH /api/invoices/{id}/status - Update invoice status to RECEIVED when old invoice status is not PENDING should return Conflict")
            void testUpdateInvoiceStatus_OldNotPending_shouldReturnConflict() throws Exception {
                UUID invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000002");
                InvoiceStatus newStatus = InvoiceStatus.CANCELED;

                mockMvc.perform(patch("/api/invoices/"+invoiceId+"/status")
                        .param("status", newStatus.name()))
                        .andExpect(status().isConflict());
            }
        }
    }

    // == Pruebas para POST /api/invoices/csv == //

    @Nested
    @DisplayName("POST /api/invoices/csv")
    class CreateInvoicesFromCSV {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("POST /api/invoices/csv - Valid CSV should return created invoices")
            void testReadInvoicesFromCSV_ValidCSV_ShouldReturnCreatedInvoices() throws Exception {
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

                UUID productInfoId3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

                mockMvc.perform(get("/api/product-info"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content[0].productInfoId").value(productInfoId2.toString()))
                        .andExpect(jsonPath("$.content[0].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO2))
                        .andExpect(jsonPath("$.content[0].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO2))
                        .andExpect(jsonPath("$.content[1].productInfoId").value(productInfoId3.toString()))
                        .andExpect(jsonPath("$.content[1].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO3))
                        .andExpect(jsonPath("$.content[1].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO3))
                        .andExpect(jsonPath("$.content[2].productInfoId").value(productInfoId1.toString()))
                        .andExpect(jsonPath("$.content[2].stockQuantity").value(OLD_QUANTITY_PRODUCTINFO1))
                        .andExpect(jsonPath("$.content[2].lastPurchaseUnitPrice").value(OLD_LAST_PURCHASE_UNIT_PRICE_PRODUCTINFO1));
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("POST /api/invoices/csv - Valid CSV with non custom product not in database should return created invoices")
            void testReadInvoicesFromCSV_NotPending_shouldReturnCreatedInvoices() throws Exception {
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
            @DisplayName("POST /api/invoices/csv - Valid CSV with non custom product not in database should return created invoices")
            void testReadInvoicesFromCSV_WithNonCustomProductNotInDatabase_ShouldReturnCreatedInvoices() throws Exception {
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
            void testReadInvoicesFromCSV_NoHeaders_ShouldReturnCreatedInvoices() throws Exception {
                MockMultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                    ("FAC-2026-001,Supplier B,PENDING,2026-03-11,20000000,12,0.95,").getBytes());

                mockMvc.perform(MockMvcRequestBuilders.multipart("/api/invoices/csv")
                        .file(csvContent))
                        .andExpect(status().isOk());
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("POST /api/invoices/csv - CSV with mismatching supplier names should return Bad Request")
            void testReadInvoicesFromCSV_SupplierMismatch_ShouldReturnBadRequest() throws Exception {
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
            @DisplayName("POST /api/invoices/csv - CSV with mismatching status should return Bad Request")
            void testReadInvoicesFromCSV_StatusMismatch_ShouldReturnBadRequest() throws Exception {
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
            @DisplayName("POST /api/invoices/csv - CSV with mismatching invoice dates should return Bad Request")
            void testReadInvoicesFromCSV_DateMismatch_ShouldReturnBadRequest() throws Exception {
                MockMultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                    ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                    "FAC-2026-001,Supplier B,PENDING,2026-03-12,20000000,12,0.95,\n" +
                    "FAC-2026-001,Supplier B,PENDING,2026-03-11,20000000,12,0.95,").getBytes());

                mockMvc.perform(MockMvcRequestBuilders.multipart("/api/invoices/csv")
                        .file(csvContent))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("POST /api/invoices/csv - Empty CSV should return Bad Request")
            void testReadInvoicesFromCSV_EmptyFile_ShouldReturnBadRequest() throws Exception {

                MockMultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                    ("").getBytes());

                mockMvc.perform(MockMvcRequestBuilders.multipart("/api/invoices/csv")
                        .file(csvContent))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @WithUserDetails("admin@expmatik.com")
            @DisplayName("POST /api/invoices/csv - CSV with invalid file extension should return Bad Request")
            void testReadInvoicesFromCSV_NoCsvFileExtension_ShouldReturnBadRequest() throws Exception {
                MockMultipartFile csvContent = new MockMultipartFile("file", "invoices.txt", "text/csv",
                    ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                    "FAC-2026-001,Supplier B,PENDING,2026-03-11,20000000,12,0.95,").getBytes());

                mockMvc.perform(MockMvcRequestBuilders.multipart("/api/invoices/csv")
                        .file(csvContent))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @WithUserDetails("repo@expmatik.com")
            @DisplayName("POST /api/invoices/csv - Unauthorized user should return Forbidden")
            void testReadInvoicesFromCSV_UnauthorizedUser_ShouldReturnForbidden() throws Exception {
                MockMultipartFile csvContent = new MockMultipartFile("file", "invoices.csv", "text/csv",
                    ("invoiceNumber,supplierName,status,invoiceDate,productBarcode,quantity,unitPrice,expirationDate\n" +
                    "FAC-2026-001,Supplier B,PENDING,2026-03-11,20000000,12,0.95,").getBytes());

                mockMvc.perform(MockMvcRequestBuilders.multipart("/api/invoices/csv")
                        .file(csvContent))
                        .andExpect(status().isForbidden());
            }
        }
    }
}

