package com.expmatik.backend.vendingSlot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.transaction.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class VendingSlotIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // == Test Get /api/vending-slots/{id} ==

    @Test
    @DisplayName("GET /api/vending-slots/{id} - valid ID and authorized user should return vending slot details")
    @WithUserDetails("admin@expmatik.com")
    public void testGetVendingSlotById_ValidIdAndAuthorizedUser_ShouldReturnVendingSlotDetails() throws Exception {
        UUID vendingMachineId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(get("/api/vending-slots/vending-machines/{machineId}", vendingMachineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].product").exists())
                .andExpect(jsonPath("$[0].currentStock").value(0))
                .andExpect(jsonPath("$[0].maxCapacity").value(2))
                .andExpect(jsonPath("$[0].isBlocked").value(false));
    }

    @Test
    @DisplayName("GET /api/vending-slots/{id} - valid ID but unauthorized user should return 403 Forbidden")
    @WithUserDetails("admin2@expmatik.com")
    public void testGetVendingSlotById_ValidIdButUnauthorizedUser_ShouldReturn403Forbidden() throws Exception {
        UUID vendingMachineId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(get("/api/vending-slots/vending-machines/{machineId}", vendingMachineId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/vending-slots/{id} - not ADMINISTRATOR role should return 403 Forbidden")
    @WithUserDetails("repo@expmatik.com")
    public void testGetVendingSlotById_NotAdministratorRole_ShouldReturn403Forbidden() throws Exception {
        UUID vendingMachineId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(get("/api/vending-slots/vending-machines/{machineId}", vendingMachineId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/vending-slots/{id} - non-existent vending slot ID should return 404 Not Found")
    @WithUserDetails("admin@expmatik.com")
    public void testGetVendingSlotById_NonExistentId_ShouldReturn404NotFound() throws Exception {
        UUID nonExistentVendingMachineId = UUID.randomUUID();

        mockMvc.perform(get("/api/vending-slots/vending-machines/{machineId}", nonExistentVendingMachineId))
                .andExpect(status().isNotFound());
    }

    // == Test Patch /api/vending-slots/{id}/assign-product/{barcode} ==

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/assign-or-unassign-product - valid ID, valid barcode and authorized user should assign product")
    @WithUserDetails("admin@expmatik.com")
    public void testAssignProduct_ValidIdAndBarcode_ShouldAssignProduct() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String barcode = "20000003";

        mockMvc.perform(patch("/api/vending-slots/{id}/assign-or-unassign-product", vendingSlotId)
                        .param("barcode", barcode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vendingSlotId.toString()))
                .andExpect(jsonPath("$.product.barcode").value(barcode));
    }

    @Test
    @DisplayName("PATCH assign-or-unassign-product - unauthorized user should return 403")
    @WithUserDetails("admin2@expmatik.com")
    public void testAssignProduct_UnauthorizedUser_ShouldReturn403() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(patch("/api/vending-slots/{id}/assign-or-unassign-product", vendingSlotId)
                        .param("barcode", "20000001"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH assign-or-unassign-product - not ADMINISTRATOR role should return 403")
    @WithUserDetails("repo@expmatik.com")
    public void testAssignProduct_NotAdmin_ShouldReturn403() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(patch("/api/vending-slots/{id}/assign-or-unassign-product", vendingSlotId)
                        .param("barcode", "20000001"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH assign-or-unassign-product - non-existent vending slot should return 404")
    @WithUserDetails("admin@expmatik.com")
    public void testAssignProduct_NonExistentSlot_ShouldReturn404() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000099");

        mockMvc.perform(patch("/api/vending-slots/{id}/assign-or-unassign-product", id)
                        .param("barcode", "20000001"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH assign-or-unassign-product - non-existent product barcode should return 404")
    @WithUserDetails("admin@expmatik.com")
    public void testAssignProduct_InvalidBarcode_ShouldReturn404() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(patch("/api/vending-slots/{id}/assign-or-unassign-product", vendingSlotId)
                        .param("barcode", "99999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH assign-or-unassign-product - blocked slot should return 409")
    @WithUserDetails("admin@expmatik.com")
    public void testAssignProduct_BlockedSlot_ShouldReturn409() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000003");

        mockMvc.perform(patch("/api/vending-slots/{id}/assign-or-unassign-product", vendingSlotId)
                        .param("barcode", "20000001"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH assign-or-unassign-product - slot not empty should return 409")
    @WithUserDetails("admin@expmatik.com")
    public void testAssignProduct_SlotNotEmpty_ShouldReturn409() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        mockMvc.perform(patch("/api/vending-slots/{id}/assign-or-unassign-product", vendingSlotId)
                        .param("barcode", "20000001"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH assign-or-unassign-product - null barcode should unassign product")
    @WithUserDetails("admin@expmatik.com")
    public void testUnassignProduct_ShouldWork() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(patch("/api/vending-slots/{id}/assign-or-unassign-product", vendingSlotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product").doesNotExist());
    }

    // == Test Patch /api/vending-slots/{id}/block-or-unblock ==

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/block-or-unblock - valid ID, block=true and authorized user should block the slot")
    @WithUserDetails("admin@expmatik.com")
    public void testBlockVendingSlot_ShouldWork() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(patch("/api/vending-slots/{id}/block-or-unblock", vendingSlotId)
                        .param("blocked", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vendingSlotId.toString()))
                .andExpect(jsonPath("$.isBlocked").value(true));
    }

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/block-or-unblock - valid ID, block=false and authorized user should unblock the slot")
    @WithUserDetails("admin@expmatik.com")
    public void testUnblockVendingSlot_ShouldWork() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(patch("/api/vending-slots/{id}/block-or-unblock", vendingSlotId)
                        .param("blocked", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vendingSlotId.toString()))
                .andExpect(jsonPath("$.isBlocked").value(false));
    }

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/block-or-unblock - valid ID but not ADMINISTRATOR role should return 403")
    @WithUserDetails("repo@expmatik.com")
    public void testBlockVendingSlot_UnauthorizedUser_ShouldReturn403() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(patch("/api/vending-slots/{id}/block-or-unblock", vendingSlotId)
                        .param("blocked", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/block-or-unblock - valid ID but does not belong to the user should return 403")
    @WithUserDetails("admin2@expmatik.com")
    public void testBlockVendingSlot_InvalidUser_ShouldReturn403() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(patch("/api/vending-slots/{id}/block-or-unblock", vendingSlotId)
                        .param("blocked", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/block-or-unblock - non-existent vending slot should return 404")
    @WithUserDetails("admin@expmatik.com")
    public void testBlockVendingSlot_NonExistentSlot_ShouldReturn404() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000099");

        mockMvc.perform(patch("/api/vending-slots/{id}/block-or-unblock", vendingSlotId)
                        .param("blocked", "true"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/block-or-unblock - cannot unblock a slot with expired products should return 409")
    @WithUserDetails("admin@expmatik.com")
    public void testUnblockVendingSlot_ExpiredProducts_ShouldReturn409() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000004");

        mockMvc.perform(patch("/api/vending-slots/{id}/block-or-unblock", vendingSlotId)
                        .param("blocked", "false"))
                .andExpect(status().isConflict());
    }

    // == Test Patch /api/vending-slots/{id}/increment-stock ==

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/increment-stock - valid ID, valid quantity, valid expiration date and authorized user should increment stock")
    @WithUserDetails("admin@expmatik.com")
    public void testIncrementVendingSlotStock_ShouldWork() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        int quantity = 1;
        LocalDate expirationDate = LocalDate.now().plusDays(3);

        mockMvc.perform(patch("/api/vending-slots/{id}/increment-stock", vendingSlotId)
                        .param("quantity", String.valueOf(quantity))
                        .param("expirationDate", expirationDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vendingSlotId.toString()))
                .andExpect(jsonPath("$.currentStock").value(quantity));
    }

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/increment-stock - valid ID, valid quantity, existing expiration date and authorized user should increment stock in existing batch")
    @WithUserDetails("admin@expmatik.com")
    public void testIncrementVendingSlotStock_ExistingExpirationDate_ShouldIncrementStock() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        int quantity = 1;
        LocalDate expirationDate = LocalDate.of(2050, 11, 30);

        mockMvc.perform(patch("/api/vending-slots/{id}/increment-stock", vendingSlotId)
                        .param("quantity", String.valueOf(quantity))
                        .param("expirationDate", expirationDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vendingSlotId.toString()))
                .andExpect(jsonPath("$.currentStock").value(4+quantity));
    }


    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/increment-stock - valid ID but not ADMINISTRATOR role should return 403")
    @WithUserDetails("repo@expmatik.com")
    public void testIncrementVendingSlotStock_UnauthorizedUser_ShouldReturn403() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        int quantity = 1;
        LocalDate expirationDate = LocalDate.now().plusDays(3);

        mockMvc.perform(patch("/api/vending-slots/{id}/increment-stock", vendingSlotId)
                        .param("quantity", String.valueOf(quantity))
                        .param("expirationDate", expirationDate.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/increment-stock - valid ID but does not belong to the user should return 403")
    @WithUserDetails("repo@expmatik.com")
    public void testIncrementVendingSlotStock_InvalidUser_ShouldReturn403() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        int quantity = 1;
        LocalDate expirationDate = LocalDate.now().plusDays(3);

        mockMvc.perform(patch("/api/vending-slots/{id}/increment-stock", vendingSlotId)
                        .param("quantity", String.valueOf(quantity))
                        .param("expirationDate", expirationDate.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/increment-stock - non-existent vending slot should return 404")
    @WithUserDetails("admin@expmatik.com")
    public void testIncrementVendingSlotStock_NonExistentSlot_ShouldReturn404() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        int quantity = 1;
        LocalDate expirationDate = LocalDate.now().plusDays(3);

        mockMvc.perform(patch("/api/vending-slots/{id}/increment-stock", vendingSlotId)
                        .param("quantity", String.valueOf(quantity))
                        .param("expirationDate", expirationDate.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/increment-stock - incrementing stock beyond max capacity should return 409")
    @WithUserDetails("admin@expmatik.com")
    public void testIncrementVendingSlotStock_BeyondMaxCapacity_ShouldReturn409() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        int quantity = 5;
        LocalDate expirationDate = LocalDate.now().plusDays(3);

        mockMvc.perform(patch("/api/vending-slots/{id}/increment-stock", vendingSlotId)
                        .param("quantity", String.valueOf(quantity))
                        .param("expirationDate", expirationDate.toString()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/increment-stock - product not assigned to slot should return 409")
    @WithUserDetails("admin@expmatik.com")
    public void testIncrementVendingSlotStock_ProductNotAssigned_ShouldReturn409() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000005");
        int quantity = 1;
        LocalDate expirationDate = LocalDate.now().plusDays(3);

        mockMvc.perform(patch("/api/vending-slots/{id}/increment-stock", vendingSlotId)
                        .param("quantity", String.valueOf(quantity))
                        .param("expirationDate", expirationDate.toString()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/increment-stock - blocked slot should return 409")
    @WithUserDetails("admin@expmatik.com")
    public void testIncrementVendingSlotStock_BlockedSlot_ShouldReturn409() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        int quantity = 1;
        LocalDate expirationDate = LocalDate.now().plusDays(3);

        mockMvc.perform(patch("/api/vending-slots/{id}/increment-stock", vendingSlotId)
                        .param("quantity", String.valueOf(quantity))
                        .param("expirationDate", expirationDate.toString()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/increment-stock - expiration date in the past should return 409")
    @WithUserDetails("admin@expmatik.com")
    public void testIncrementVendingSlotStock_PastExpirationDate_ShouldReturn409() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        int quantity = 1;
        LocalDate expirationDate = LocalDate.now().minusDays(1);

        mockMvc.perform(patch("/api/vending-slots/{id}/increment-stock", vendingSlotId)
                        .param("quantity", String.valueOf(quantity))
                        .param("expirationDate", expirationDate.toString()))
                .andExpect(status().isConflict());
    }

    // == Test Patch /api/vending-slots/{id}/decrement-stock ==

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/decrement-stock - valid ID, valid quantity and authorized user should decrement stock")
    @WithUserDetails("admin@expmatik.com")
    public void testDecrementVendingSlotStock_ValidIdAndValidQuantityAndAuthorizedUser_ShouldDecrementStockAndCreateNotification() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000003");

        mockMvc.perform(patch("/api/vending-slots/{id}/decrement-stock", vendingSlotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vendingSlotId.toString()))
                .andExpect(jsonPath("$.currentStock").value(3));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].type").value("PRODUCT_LOW_STOCK"))
                .andExpect(jsonPath("$.content[0].isRead").value(false))
                .andExpect(jsonPath("$.content[0].createdAt").value(Matchers.startsWith(LocalDate.now().toString())));
    }

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/decrement-stock - valid ID, valid quantity and authorized user should decrement stock and delete expiration batch if stock reaches zero")
    @WithUserDetails("admin@expmatik.com")
    public void testDecrementVendingSlotStock_ValidIdAndValidQuantityAndAuthorizedUser_ShouldDecrementStockAndDeleteExpirationBatchIfStockReachesZeroAndCreateNotification() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000007");

        mockMvc.perform(patch("/api/vending-slots/{id}/decrement-stock", vendingSlotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vendingSlotId.toString()))
                .andExpect(jsonPath("$.currentStock").value(0));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].type").value("PRODUCT_OUT_OF_STOCK"))
                .andExpect(jsonPath("$.content[0].isRead").value(false))
                .andExpect(jsonPath("$.content[0].createdAt").value(Matchers.startsWith(LocalDate.now().toString())));

    }

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/decrement-stock - valid ID but not ADMINISTRATOR role should return 403")
    @WithUserDetails("repo@expmatik.com")
    public void testDecrementVendingSlotStock_ValidIdButNotAdministratorRole_ShouldReturn403() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000003");

        mockMvc.perform(patch("/api/vending-slots/{id}/decrement-stock", vendingSlotId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/decrement-stock - valid ID but does not belong to the user should return 403")
    @WithUserDetails("admin2@expmatik.com")
    public void testDecrementVendingSlotStock_ValidIdButDoesNotBelongToUser_ShouldReturn403() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000003");

        mockMvc.perform(patch("/api/vending-slots/{id}/decrement-stock", vendingSlotId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/decrement-stock - non-existent vending slot should return 404")
    @WithUserDetails("admin@expmatik.com")
    public void testDecrementVendingSlotStock_NonExistentVendingSlot_ShouldReturn404() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000099");

        mockMvc.perform(patch("/api/vending-slots/{id}/decrement-stock", vendingSlotId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/decrement-stock - decrementing stock below zero should return 409")
    @WithUserDetails("admin@expmatik.com")
    public void testDecrementVendingSlotStock_DecrementingBelowZero_ShouldReturn409() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(patch("/api/vending-slots/{id}/decrement-stock", vendingSlotId))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/decrement-stock - blocked slot should return 409")
    @WithUserDetails("admin@expmatik.com")
    public void testDecrementVendingSlotStock_BlockedSlot_ShouldReturn409() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000006");

        mockMvc.perform(patch("/api/vending-slots/{id}/decrement-stock", vendingSlotId))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/decrement-stock - product not assigned to slot should return 409")
    @WithUserDetails("admin@expmatik.com")
    public void testDecrementVendingSlotStock_ProductNotAssigned_ShouldReturn409() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000005");

        mockMvc.perform(patch("/api/vending-slots/{id}/decrement-stock", vendingSlotId))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH /api/vending-slots/{id}/decrement-stock - expired products should return 409")
    @WithUserDetails("admin@expmatik.com")
    public void testDecrementVendingSlotStock_ExpiredProducts_ShouldReturn409() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000004");

        mockMvc.perform(patch("/api/vending-slots/{id}/decrement-stock", vendingSlotId))
                .andExpect(status().isConflict());
    }

    // == Test Get /api/vending-slots/{vendingSlotId}/expiration-batches ==

    @Test
    @DisplayName("GET /api/vending-slots/{vendingSlotId}/expiration-batches - valid vending slot ID and authorized user should return expiration batches")
    @WithUserDetails("admin@expmatik.com")
    public void testGetVendingSlotExpirationBatches_ValidIdAndAuthorizedUser_ShouldReturnExpirationBatches() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000004");

        mockMvc.perform(get("/api/vending-slots/{id}/expiration-batches", vendingSlotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/vending-slots/{vendingSlotId}/expiration-batches - valid vending slot ID with no expiration batches should return empty list")
    @WithUserDetails("admin@expmatik.com")
    public void testGetVendingSlotExpirationBatches_ValidIdWithNoBatches_ShouldReturnEmptyList() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(get("/api/vending-slots/{id}/expiration-batches", vendingSlotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/vending-slots/{vendingSlotId}/expiration-batches - valid vending slot ID but not ADMINISTRATOR role should return 403")
    @WithUserDetails("repo@expmatik.com")
    public void testGetVendingSlotExpirationBatches_ValidIdButNotAdmin_ShouldReturn403() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000004");

        mockMvc.perform(get("/api/vending-slots/{id}/expiration-batches", vendingSlotId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/vending-slots/{vendingSlotId}/expiration-batches - valid vending slot ID but unauthorized user should return 403")
    @WithUserDetails("admin2@expmatik.com")
    public void testGetVendingSlotExpirationBatches_ValidIdButUnauthorizedUser_ShouldReturn403() throws Exception {
        UUID vendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000004");

        mockMvc.perform(get("/api/vending-slots/{id}/expiration-batches", vendingSlotId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/vending-slots/{vendingSlotId}/expiration-batches - non-existent vending slot ID should return 404")
    @WithUserDetails("admin@expmatik.com")
    public void testGetVendingSlotExpirationBatches_NonExistentId_ShouldReturn404() throws Exception {
        UUID nonExistentVendingSlotId = UUID.fromString("00000000-0000-0000-0000-000000000099");

        mockMvc.perform(get("/api/vending-slots/{id}/expiration-batches", nonExistentVendingSlotId))
                .andExpect(status().isNotFound());
    }

    

}
