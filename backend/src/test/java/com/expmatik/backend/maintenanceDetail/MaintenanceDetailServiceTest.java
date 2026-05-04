package com.expmatik.backend.maintenanceDetail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.expmatik.backend.exceptions.BadRequestException;
import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.maintenance.Maintenance;
import com.expmatik.backend.maintenance.MaintenanceStatus;
import com.expmatik.backend.maintenanceDetail.DTOs.MaintenanceDetailCreate;
import com.expmatik.backend.product.Product;
import com.expmatik.backend.productInfo.ProductInfo;
import com.expmatik.backend.productInfo.ProductInfoService;
import com.expmatik.backend.user.Role;
import com.expmatik.backend.user.User;
import com.expmatik.backend.vendingMachine.VendingMachine;
import com.expmatik.backend.vendingSlot.VendingSlot;
import com.expmatik.backend.vendingSlot.VendingSlotService;

@ExtendWith(MockitoExtension.class)
public class MaintenanceDetailServiceTest {

    @Mock
    private MaintenanceDetailRepository maintenanceDetailRepository;

    @Mock
    private VendingSlotService vendingSlotService;

    @Mock
    private ProductInfoService productInfoService;

    @Spy
    @InjectMocks
    private MaintenanceDetailService maintenanceDetailService;

    private Maintenance maintenance;
    private User maintainer;
    private User administrator;
    private User otherUser;
    private List<MaintenanceDetail> maintenanceDetails;
    private Product product;
    private VendingMachine vendingMachine;
    private VendingSlot vendingSlot;
    private VendingSlot vendingSlot2;
    private ProductInfo productInfo;

    @BeforeEach
    void setUp() {
        maintainer = new User();
        maintainer.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        maintainer.setRole(Role.MAINTAINER);

        administrator = new User();
        administrator.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        administrator.setRole(Role.ADMINISTRATOR);

        otherUser = new User();
        otherUser.setId(UUID.fromString("00000000-0000-0000-0000-000000000003"));
        otherUser.setRole(Role.MAINTAINER);

        product = new Product();
        product.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        product.setName("Test Product");
        product.setBarcode("1234567890123");
        product.setIsPerishable(true);

        vendingMachine = new VendingMachine();
        vendingMachine.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        vendingMachine.setName("Test Vending Machine");

        vendingSlot = new VendingSlot();
        vendingSlot.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        vendingSlot.setRowNumber(1);
        vendingSlot.setColumnNumber(1);
        vendingSlot.setProduct(product);
        vendingSlot.setVendingMachine(vendingMachine);
        vendingSlot.setCurrentStock(1);
        vendingSlot.setMaxCapacity(10);


        vendingSlot2 = new VendingSlot();
        vendingSlot2.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        vendingSlot2.setRowNumber(2);
        vendingSlot2.setColumnNumber(2);
        vendingSlot2.setProduct(product);
        vendingSlot2.setVendingMachine(vendingMachine);
        vendingSlot2.setCurrentStock(1);
        vendingSlot2.setMaxCapacity(10);

        productInfo = new ProductInfo();
        productInfo.setId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        productInfo.setProduct(product);
        productInfo.setUser(maintainer);
        productInfo.setStockQuantity(10);

        MaintenanceDetail detail = new MaintenanceDetail();
        detail.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        detail.setQuantityToRestock(1);
        detail.setRowNumber(1);
        detail.setColumnNumber(1);
        detail.setProduct(product);

        maintenanceDetails = new LinkedList<>();
        maintenanceDetails.add(detail);

        maintenance = new Maintenance();
        maintenance.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        maintenance.setMaintenanceDate(java.time.LocalDate.now());
        maintenance.setStatus(MaintenanceStatus.DRAFT);
        maintenance.setDescription("Test maintenance");
        maintenance.setMaintainer(maintainer);
        maintenance.setAdministrator(administrator);
        maintenance.setVendingMachine(vendingMachine);
        maintenance.setMaintenanceDetails(maintenanceDetails);
    }

    @Nested
    @DisplayName("createMaintenanceDetail")
    class CreateMaintenanceDetailTests {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("Should create maintenance detail successfully")
            void createMaintenanceDetail_ValidRequest_ReturnsCreatedDetail() {
                MaintenanceDetailCreate createRequest = new MaintenanceDetailCreate(
                        1,
                        LocalDate.now().plusDays(1),
                        1,
                        2,
                        product.getBarcode()
                );

                when(vendingSlotService.getVendingSlotByMachineNameAndRowAndColumn(
                        vendingMachine.getName(),
                        createRequest.rowNumber(),
                        createRequest.columnNumber(),
                        maintainer
                )).thenReturn(vendingSlot);
                when(maintenanceDetailRepository.sumQuantityToRestockByMaintenanceIdAndSlotCoordinates(
                        maintenance.getId(),
                        vendingMachine.getName(),
                        createRequest.rowNumber(),
                        createRequest.columnNumber()
                )).thenReturn(0);
                when(productInfoService.getOrCreateProductInfo(product.getId(), maintainer, null)).thenReturn(productInfo);
                when(productInfoService.editStockQuantity(eq(productInfo.getId()), eq(maintainer), eq(-createRequest.quantityToRestock()), eq(null))).thenReturn(productInfo);

                MaintenanceDetail result = maintenanceDetailService.createMaintenanceDetail(maintenance, createRequest, maintainer);

                assertNotNull(result);
                assertEquals(createRequest.quantityToRestock(), result.getQuantityToRestock());
                assertEquals(createRequest.rowNumber(), result.getRowNumber());
                assertEquals(createRequest.columnNumber(), result.getColumnNumber());
                assertEquals(vendingSlot.getProduct(), result.getProduct());
            }

            @Test
            @DisplayName("Should create maintenance detail successfully distinct slot without previous details")
            void createMaintenanceDetail_DistinctSlotNoPreviousDetails_ReturnsCreatedDetail() {
                MaintenanceDetailCreate createRequest = new MaintenanceDetailCreate(
                        1,
                        LocalDate.now().plusDays(1),
                        2,
                        2,
                        product.getBarcode()
                );

                when(vendingSlotService.getVendingSlotByMachineNameAndRowAndColumn(
                        vendingMachine.getName(),
                        createRequest.rowNumber(),
                        createRequest.columnNumber(),
                        maintainer
                )).thenReturn(vendingSlot2);
                when(maintenanceDetailRepository.sumQuantityToRestockByMaintenanceIdAndSlotCoordinates(
                        maintenance.getId(),
                        vendingMachine.getName(),
                        createRequest.rowNumber(),
                        createRequest.columnNumber()
                )).thenReturn(0);
                when(productInfoService.getOrCreateProductInfo(product.getId(), maintainer, null)).thenReturn(productInfo);
                when(productInfoService.editStockQuantity(eq(productInfo.getId()), eq(maintainer), eq(-createRequest.quantityToRestock()), eq(null))).thenReturn(productInfo);

                MaintenanceDetail result = maintenanceDetailService.createMaintenanceDetail(maintenance, createRequest, maintainer);

                assertNotNull(result);
                assertEquals(createRequest.quantityToRestock(), result.getQuantityToRestock());
                assertEquals(createRequest.rowNumber(), result.getRowNumber());
                assertEquals(createRequest.columnNumber(), result.getColumnNumber());
                assertEquals(vendingSlot2.getProduct(), result.getProduct());
                
            }
            @Test
            @DisplayName("Should create maintenance detail successfully with Product dont expires")
            void createMaintenanceDetail_ProductDoesntExpire_ReturnsCreatedDetail() {
                MaintenanceDetailCreate createRequest = new MaintenanceDetailCreate(
                        1,
                        null,
                        1,
                        1,
                        product.getBarcode()
                );
                product.setIsPerishable(false);

                when(vendingSlotService.getVendingSlotByMachineNameAndRowAndColumn(
                        vendingMachine.getName(),
                        createRequest.rowNumber(),
                        createRequest.columnNumber(),
                        maintainer
                )).thenReturn(vendingSlot);
                when(maintenanceDetailRepository.sumQuantityToRestockByMaintenanceIdAndSlotCoordinates(
                        maintenance.getId(),
                        vendingMachine.getName(),
                        createRequest.rowNumber(),
                        createRequest.columnNumber()
                )).thenReturn(1);
                when(productInfoService.getOrCreateProductInfo(product.getId(), maintainer, null)).thenReturn(productInfo);
                when(productInfoService.editStockQuantity(eq(productInfo.getId()), eq(maintainer), eq(-createRequest.quantityToRestock()), eq(null))).thenReturn(productInfo);

                MaintenanceDetail result = maintenanceDetailService.createMaintenanceDetail(maintenance, createRequest, maintainer);

                assertNotNull(result);
                assertEquals(createRequest.quantityToRestock(), result.getQuantityToRestock());
                assertEquals(createRequest.rowNumber(), result.getRowNumber());
                assertEquals(createRequest.columnNumber(), result.getColumnNumber());
                assertEquals(vendingSlot.getProduct(), result.getProduct());
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("Should throw BadRequestException when barcode does not match product in vending slot")
            void createMaintenanceDetail_BarcodeMismatch_ThrowsBadRequestException() {
                MaintenanceDetailCreate createRequest = new MaintenanceDetailCreate(
                        1,
                        LocalDate.now().plusDays(1),
                        1,
                        1,
                        "invalid_barcode"
                );

                when(vendingSlotService.getVendingSlotByMachineNameAndRowAndColumn(
                        vendingMachine.getName(),
                        createRequest.rowNumber(),
                        createRequest.columnNumber(),
                        maintainer
                )).thenReturn(vendingSlot);

                assertThrows(BadRequestException.class, () -> {
                    maintenanceDetailService.createMaintenanceDetail(maintenance, createRequest, maintainer);
                });
            }

            @Test
            @DisplayName("Should throw BadRequestException when Product expires and Expiration date dont provided")
            void createMaintenanceDetail_ProductExpiresAndExpirationDateNotProvided_ThrowsBadRequestException() {
                MaintenanceDetailCreate createRequest = new MaintenanceDetailCreate(
                        1,
                        null,
                        1,
                        1,
                        product.getBarcode()
                );

                when(vendingSlotService.getVendingSlotByMachineNameAndRowAndColumn(
                        vendingMachine.getName(),
                        createRequest.rowNumber(),
                        createRequest.columnNumber(),
                        maintainer
                )).thenReturn(vendingSlot);

                assertThrows(BadRequestException.class, () -> {
                    maintenanceDetailService.createMaintenanceDetail(maintenance, createRequest, maintainer);
                });
            }

            @Test
            @DisplayName("Should throw BadRequestException when Product not expires and Expiration date is provided")
            void createMaintenanceDetail_ProductNotExpireAndExpirationDateProvided_ThrowsBadRequestException() {
                MaintenanceDetailCreate createRequest = new MaintenanceDetailCreate(
                        1,
                        LocalDate.now().plusDays(1),
                        1,
                        1,
                        product.getBarcode()
                );
                product.setIsPerishable(false);

                when(vendingSlotService.getVendingSlotByMachineNameAndRowAndColumn(
                        vendingMachine.getName(),
                        createRequest.rowNumber(),
                        createRequest.columnNumber(),
                        maintainer
                )).thenReturn(vendingSlot);

                assertThrows(BadRequestException.class, () -> {
                    maintenanceDetailService.createMaintenanceDetail(maintenance, createRequest, maintainer);
                });
            }

            @Test
            @DisplayName("Should throw ConflictException when inventory stock is insufficient")
            void createMaintenanceDetail_InsufficientInventoryStock_ThrowsConflictException() {
                MaintenanceDetailCreate createRequest = new MaintenanceDetailCreate(
                        3,
                        LocalDate.now().plusDays(1),
                        1,
                        1,
                        product.getBarcode()
                );

                productInfo.setStockQuantity(2);

                when(vendingSlotService.getVendingSlotByMachineNameAndRowAndColumn(
                        vendingMachine.getName(),
                        createRequest.rowNumber(),
                        createRequest.columnNumber(),
                        maintainer
                )).thenReturn(vendingSlot);
                when(productInfoService.getOrCreateProductInfo(product.getId(), maintainer, null)).thenReturn(productInfo);

                assertThrows(ConflictException.class, () -> {
                    maintenanceDetailService.createMaintenanceDetail(maintenance, createRequest, maintainer);
                });
            }

                        @Test
                        @DisplayName("Should throw ConflictException when total maintenance details exceed slot capacity")
                        void createMaintenanceDetail_TotalQuantityExceedsSlotCapacity_ThrowsConflictException() {
                                MaintenanceDetailCreate createRequest = new MaintenanceDetailCreate(
                                                9,
                                                LocalDate.now().plusDays(1),
                                                1,
                                                1,
                                                product.getBarcode()
                                );

                                when(vendingSlotService.getVendingSlotByMachineNameAndRowAndColumn(
                                                vendingMachine.getName(),
                                                createRequest.rowNumber(),
                                                createRequest.columnNumber(),
                                                maintainer
                                )).thenReturn(vendingSlot);
                        when(maintenanceDetailRepository.sumQuantityToRestockByMaintenanceIdAndSlotCoordinates(
                                maintenance.getId(),
                                vendingMachine.getName(),
                                createRequest.rowNumber(),
                                createRequest.columnNumber()
                        )).thenReturn(1);

                                assertThrows(ConflictException.class, () -> {
                                        maintenanceDetailService.createMaintenanceDetail(maintenance, createRequest, maintainer);
                                });

                                verify(productInfoService, never()).getOrCreateProductInfo(any(), any(), any());
                        }

            @Test
            @DisplayName("Should throw BadRequestException when vending slot does not have a product assigned")
            void createMaintenanceDetail_VendingSlotNoProduct_ThrowsBadRequestException() {
                MaintenanceDetailCreate createRequest = new MaintenanceDetailCreate(
                        1,
                        LocalDate.now().plusDays(1),
                        1,
                        1,
                        product.getBarcode()
                );
                vendingSlot.setProduct(null);

                when(vendingSlotService.getVendingSlotByMachineNameAndRowAndColumn(
                        vendingMachine.getName(),
                        createRequest.rowNumber(),
                        createRequest.columnNumber(),
                        maintainer
                )).thenReturn(vendingSlot);

                assertThrows(BadRequestException.class, () -> {
                    maintenanceDetailService.createMaintenanceDetail(maintenance, createRequest, maintainer);
                });
            }

            @Test
            @DisplayName("Should throw BadRequestException when expiration date is before maintenance date")
            void createMaintenanceDetail_PastExpirationDate_ThrowsBadRequestException() {

                MaintenanceDetailCreate createRequest = new MaintenanceDetailCreate(
                        1,
                        LocalDate.now().minusDays(1),
                        1,
                        1,
                        product.getBarcode()
                );

                when(vendingSlotService.getVendingSlotByMachineNameAndRowAndColumn(
                        vendingMachine.getName(),
                        createRequest.rowNumber(),
                        createRequest.columnNumber(),
                        maintainer
                )).thenReturn(vendingSlot);

                assertThrows(BadRequestException.class, () -> {
                    maintenanceDetailService.createMaintenanceDetail(maintenance, createRequest, maintainer);
                });
            }
        }
    }

    @Nested
    @DisplayName("performSlotsMaintenance")
    class PerformSlotsMaintenanceTests {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("Should restock full quantity when slot has enough space")
            void performSlotsMaintenance_SlotHasEnoughSpace_FullQuantityRestocked() {
                MaintenanceDetail detail = maintenanceDetails.get(0);
                detail.setQuantityToRestock(5);
                // vendingSlot: currentStock=1, maxCapacity=10 → remainingSpace=9 → full fit

                when(maintenanceDetailRepository.findDistinctVendingSlotsByMaintenance(maintenance.getId()))
                        .thenReturn(List.of(vendingSlot));
                when(maintenanceDetailRepository.findMaintenanceDetailsByMaintenanceIdAndSlotCoordinates(
                        maintenance.getId(), vendingSlot.getVendingMachine().getName(),
                        vendingSlot.getRowNumber(), vendingSlot.getColumnNumber()))
                        .thenReturn(List.of(detail));

                maintenanceDetailService.performSlotsMaintenance(maintenance, maintainer);

                verify(vendingSlotService).addStockToVendingSlot(
                        eq(vendingSlot.getId()), eq(5), eq(detail.getExpirationDate()), eq(maintainer));
                verify(productInfoService, never()).getOrCreateProductInfo(any(), any(), any());
                assertEquals(5, detail.getQuantityRestocked());
                assertEquals(0, detail.getQuantityReturned());
            }

            @Test
            @DisplayName("Should partially restock and return overflow to inventory when slot is partially full")
            void performSlotsMaintenance_SlotPartiallyFull_PartialRestockAndOverflowReturned() {
                MaintenanceDetail detail = maintenanceDetails.get(0);
                detail.setQuantityToRestock(5);
                vendingSlot.setCurrentStock(7); // remainingSpace = 10 - 7 = 3 → add 3, return 2

                when(maintenanceDetailRepository.findDistinctVendingSlotsByMaintenance(maintenance.getId()))
                        .thenReturn(List.of(vendingSlot));
                when(maintenanceDetailRepository.findMaintenanceDetailsByMaintenanceIdAndSlotCoordinates(
                        maintenance.getId(), vendingSlot.getVendingMachine().getName(),
                        vendingSlot.getRowNumber(), vendingSlot.getColumnNumber()))
                        .thenReturn(List.of(detail));
                when(productInfoService.getOrCreateProductInfo(product.getId(), administrator, null))
                        .thenReturn(productInfo);
                when(productInfoService.editStockQuantity(eq(productInfo.getId()), eq(administrator), eq(2), eq(null)))
                        .thenReturn(productInfo);

                maintenanceDetailService.performSlotsMaintenance(maintenance, maintainer);

                verify(vendingSlotService).addStockToVendingSlot(
                        eq(vendingSlot.getId()), eq(3), eq(detail.getExpirationDate()), eq(maintainer));
                verify(productInfoService).editStockQuantity(
                        eq(productInfo.getId()), eq(administrator), eq(2), eq(null));
                assertEquals(3, detail.getQuantityRestocked());
                assertEquals(2, detail.getQuantityReturned());
            }

            @Test
            @DisplayName("Should return all quantity to inventory when product expiration date is in the past")
            void performSlotsMaintenance_ExpiredProduct_AllQuantityReturnedToInventory() {
                MaintenanceDetail detail = maintenanceDetails.get(0);
                detail.setQuantityToRestock(3);
                detail.setExpirationDate(LocalDate.now().minusDays(1)); // expired yesterday
                // vendingSlot: currentStock=1, maxCapacity=10 → space available but product is expired

                when(maintenanceDetailRepository.findDistinctVendingSlotsByMaintenance(maintenance.getId()))
                        .thenReturn(List.of(vendingSlot));
                when(maintenanceDetailRepository.findMaintenanceDetailsByMaintenanceIdAndSlotCoordinates(
                        maintenance.getId(), vendingSlot.getVendingMachine().getName(),
                        vendingSlot.getRowNumber(), vendingSlot.getColumnNumber()))
                        .thenReturn(List.of(detail));
                when(productInfoService.getOrCreateProductInfo(product.getId(), administrator, null))
                        .thenReturn(productInfo);
                when(productInfoService.editStockQuantity(eq(productInfo.getId()), eq(administrator), eq(3), eq(null)))
                        .thenReturn(productInfo);

                maintenanceDetailService.performSlotsMaintenance(maintenance, maintainer);

                verify(vendingSlotService, never()).addStockToVendingSlot(any(), any(), any(), any());
                verify(productInfoService).editStockQuantity(
                        eq(productInfo.getId()), eq(administrator), eq(3), eq(null));
                assertEquals(0, detail.getQuantityRestocked());
                assertEquals(3, detail.getQuantityReturned());
            }

            @Test
            @DisplayName("Should return all quantity to inventory when slot is completely full")
            void performSlotsMaintenance_SlotCompletelyFull_AllQuantityReturnedToInventory() {
                MaintenanceDetail detail = maintenanceDetails.get(0);
                detail.setQuantityToRestock(5);
                vendingSlot.setCurrentStock(10); // remainingSpace = 0 → add 0, return 5

                when(maintenanceDetailRepository.findDistinctVendingSlotsByMaintenance(maintenance.getId()))
                        .thenReturn(List.of(vendingSlot));
                when(maintenanceDetailRepository.findMaintenanceDetailsByMaintenanceIdAndSlotCoordinates(
                        maintenance.getId(), vendingSlot.getVendingMachine().getName(),
                        vendingSlot.getRowNumber(), vendingSlot.getColumnNumber()))
                        .thenReturn(List.of(detail));
                when(productInfoService.getOrCreateProductInfo(product.getId(), administrator, null))
                        .thenReturn(productInfo);
                when(productInfoService.editStockQuantity(eq(productInfo.getId()), eq(administrator), eq(5), eq(null)))
                        .thenReturn(productInfo);

                maintenanceDetailService.performSlotsMaintenance(maintenance, maintainer);

                verify(vendingSlotService, never()).addStockToVendingSlot(any(), any(), any(), any());
                verify(productInfoService).editStockQuantity(
                        eq(productInfo.getId()), eq(administrator), eq(5), eq(null));
                assertEquals(0, detail.getQuantityRestocked());
                assertEquals(5, detail.getQuantityReturned());
            }
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("Should return maintenance detail when found")
            void findById_ExistingId_ReturnsMaintenanceDetail() {
                UUID detailId = maintenanceDetails.get(0).getId();
                when(maintenanceDetailRepository.findById(detailId)).thenReturn(java.util.Optional.of(maintenanceDetails.get(0)));

                MaintenanceDetail result = maintenanceDetailService.findById(detailId);
                assertNotNull(result);
                assertEquals(detailId, result.getId());
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("Should throw ResourceNotFoundException when maintenance detail not found")
            void findById_NonExistingId_ThrowsResourceNotFoundException() {
                UUID nonExistingId = UUID.fromString("00000000-0000-0000-0000-000000000999");
                when(maintenanceDetailRepository.findById(nonExistingId)).thenReturn(java.util.Optional.empty());

                assertThrows(com.expmatik.backend.exceptions.ResourceNotFoundException.class, () -> {
                    maintenanceDetailService.findById(nonExistingId);
                });
            }
        }
    }

}
