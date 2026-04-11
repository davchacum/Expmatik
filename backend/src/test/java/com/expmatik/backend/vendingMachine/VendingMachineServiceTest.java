package com.expmatik.backend.vendingMachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.user.User;
import com.expmatik.backend.vendingMachine.DTOs.VendingMachineCreate;
import com.expmatik.backend.vendingMachine.DTOs.VendingMachineUpdate;
import com.expmatik.backend.vendingSlot.VendingSlotService;

@ExtendWith(MockitoExtension.class)
public class VendingMachineServiceTest {

        @Mock
        private VendingMachineRepository vendingMachineRepository;

        @Mock
        private VendingSlotService vendingSlotService;

        @Spy
        @InjectMocks
        private VendingMachineService vendingMachineService;

        // == Test cases for createVendingMachine ==

        @Nested
        @DisplayName("createVendingMachine")
        class CreateVendingMachine {

                @Nested
                @DisplayName("Success Cases")
                class SuccessCases {

                        @Test
                        @DisplayName("createVendingMachine - success")
                                void testCreateVendingMachine_ValidData_Success() {
                                User user = new User();
                                user.setId(UUID.randomUUID());
                                Integer maxCapacityPerSlot = 10;
                                Integer rowCount = 3;
                                Integer columnCount = 4;

                                VendingMachineCreate dto = new VendingMachineCreate(
                                        "Random","Machine 1", columnCount, rowCount, maxCapacityPerSlot
                                );

                                when(vendingMachineRepository.findByNameAndUserId("Machine 1", user.getId()))
                                        .thenReturn(Optional.empty());

                                when(vendingMachineRepository.save(any(VendingMachine.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));

                                VendingMachine result = vendingMachineService.createVendingMachine(dto, user);

                                assertEquals(result.getName(),"Machine 1");
                                assertEquals(result.getUser(),user);

                                verify(vendingMachineRepository).save(any(VendingMachine.class));
                                verify(vendingMachineRepository).save(argThat(vm ->
                                        vm.getUser().equals(user) &&
                                        vm.getName().equals("Machine 1")
                                ));
                                verify(vendingSlotService).createVendingSlotsForMachine(eq(result), eq(rowCount), eq(columnCount), eq(maxCapacityPerSlot));
                        }
                }

                @Nested
                @DisplayName("Failure Cases")
                class FailureCases {

                        @Test
                        @DisplayName("createVendingMachine - name conflict")
                                void testCreateVendingMachine_DuplicateName_ShouldThrowConflictException() {
                                User user = new User();
                                user.setId(UUID.randomUUID());

                                VendingMachineCreate dto = new VendingMachineCreate(
                                        "Random","Machine 1", 3, 4, 10
                                );

                                when(vendingMachineRepository.findByNameAndUserId("Machine 1", user.getId()))
                                        .thenReturn(Optional.of(new VendingMachine()));

                                assertThrows(ConflictException.class, () -> {
                                        vendingMachineService.createVendingMachine(dto, user);
                                });

                                verify(vendingMachineRepository).findByNameAndUserId("Machine 1", user.getId());
                                verify(vendingMachineRepository).findByNameAndUserId(eq("Machine 1"), eq(user.getId()));
                        }
                }
        }

        // == Test cases for getVendingMachineById ==

        @Nested
        @DisplayName("getVendingMachineById")
        class GetVendingMachineById {

                @Nested
                @DisplayName("Success Cases")
                class SuccessCases {

                        @Test
                        @DisplayName("getVendingMachineById - success")
                                void testGetVendingMachineById_ValidId_ShouldReturnVendingMachine() {
                                User user = new User();
                                user.setId(UUID.randomUUID());

                                VendingMachine vendingMachine = new VendingMachine();
                                vendingMachine.setId(UUID.randomUUID());
                                vendingMachine.setUser(user);

                                when(vendingMachineRepository.findById(vendingMachine.getId()))
                                        .thenReturn(Optional.of(vendingMachine));

                                VendingMachine result = vendingMachineService.getVendingMachineById(vendingMachine.getId(), user);

                                assertEquals(result,vendingMachine);
                                verify(vendingMachineRepository).findById(vendingMachine.getId());
                        }
                }

                @Nested
                @DisplayName("Failure Cases")
                class FailureCases {

                        @Test
                        @DisplayName("getVendingMachineById - not found")
                                void testGetVendingMachineById_NotFound_ShouldThrowResourceNotFoundException() {
                                User user = new User();
                                user.setId(UUID.randomUUID());
                                UUID vendingMachineId = UUID.randomUUID();

                                when(vendingMachineRepository.findById(vendingMachineId))
                                        .thenReturn(Optional.empty());

                                assertThrows(ResourceNotFoundException.class, () -> {
                                        vendingMachineService.getVendingMachineById(vendingMachineId, user);
                                });

                                verify(vendingMachineRepository).findById(vendingMachineId);
                        }

                        @Test
                        @DisplayName("getVendingMachineById - access denied")
                                void testGetVendingMachineById_AccessDenied_ShouldThrowAccessDeniedException() {
                                User user = new User();
                                user.setId(UUID.randomUUID());

                                User otherUser = new User();
                                otherUser.setId(UUID.randomUUID());

                                VendingMachine vendingMachine = new VendingMachine();
                                vendingMachine.setId(UUID.randomUUID());
                                vendingMachine.setUser(otherUser);

                                when(vendingMachineRepository.findById(vendingMachine.getId()))
                                        .thenReturn(Optional.of(vendingMachine));

                                assertThrows(AccessDeniedException.class, () -> {
                                        vendingMachineService.getVendingMachineById(vendingMachine.getId(), user);
                                });

                                verify(vendingMachineRepository).findById(vendingMachine.getId());
                        }
                }
        }

        // == Test cases for updateVendingMachine ==

        @Nested
        @DisplayName("updateVendingMachine")
        class UpdateVendingMachine {

                @Nested
                @DisplayName("Success Cases")
                class SuccessCases {

                        @Test
                        @DisplayName("updateVendingMachine - success")
                        void testUpdateVendingMachine_ValidMachine_Success() {
                                User user = new User();
                                user.setId(UUID.randomUUID());

                                VendingMachine vendingMachine = new VendingMachine();
                                vendingMachine.setId(UUID.randomUUID());
                                vendingMachine.setUser(user);
                                vendingMachine.setLocation("Old Location");
                                vendingMachine.setName("Old Name");

                                VendingMachineUpdate dto = new VendingMachineUpdate(
                                        "New Location"
                                );

                                when(vendingMachineRepository.findById(vendingMachine.getId()))
                                        .thenReturn(Optional.of(vendingMachine));


                                when(vendingMachineRepository.save(any(VendingMachine.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));

                                VendingMachine result = vendingMachineService.updateVendingMachine(vendingMachine.getId(), dto, user);

                                assertEquals(result.getLocation(),"New Location");
                                assertEquals(result.getName(),"Old Name");

                                verify(vendingMachineRepository).findById(vendingMachine.getId());
                                verify(vendingMachineRepository).save(any(VendingMachine.class));
                        }
                }

                @Nested
                @DisplayName("Failure Cases")
                class FailureCases {

                        @Test
                        @DisplayName("updateVendingMachine - unauthorized machine ownership")
                        void testUpdateVendingMachine_UnauthorizedMachineOwnership_shouldThrowAccessDeniedException() {
                                User user = new User();
                                user.setId(UUID.randomUUID());

                                User otherUser = new User();
                                otherUser.setId(UUID.randomUUID());

                                VendingMachine vendingMachine = new VendingMachine();
                                vendingMachine.setId(UUID.randomUUID());
                                vendingMachine.setUser(otherUser);

                                VendingMachineUpdate dto = new VendingMachineUpdate(
                                        "New Location"
                                );

                                when(vendingMachineRepository.findById(vendingMachine.getId()))
                                        .thenReturn(Optional.of(vendingMachine));

                                assertThrows(AccessDeniedException.class, () -> {
                                        vendingMachineService.updateVendingMachine(vendingMachine.getId(), dto, user);
                                });

                                verify(vendingMachineRepository).findById(vendingMachine.getId());
                        }

                        @Test
                        @DisplayName("updateVendingMachine - machine not found")
                        void testUpdateVendingMachine_MachineNotFound_shouldThrowResourceNotFoundException() {
                                User user = new User();
                                user.setId(UUID.randomUUID());
                                UUID vendingMachineId = UUID.randomUUID();

                                VendingMachineUpdate dto = new VendingMachineUpdate(
                                        "New Location"
                                );

                                when(vendingMachineRepository.findById(vendingMachineId))
                                        .thenReturn(Optional.empty());

                                assertThrows(ResourceNotFoundException.class, () -> {
                                        vendingMachineService.updateVendingMachine(vendingMachineId, dto, user);
                                });

                                verify(vendingMachineRepository).findById(vendingMachineId);
                        }
                }
        }
        
        @Nested
        @DisplayName("findVendingMachineByNameAndUserId")
        class FindVendingMachineByNameAndUserId {

                @Nested
                @DisplayName("Success Cases")
                class SuccessCases {

                        @Test
                        @DisplayName("findVendingMachineByNameAndUserId - success")
                        void testFindVendingMachineByNameAndUserId_ValidName_Success() {
                                User user = new User();
                                user.setId(UUID.randomUUID());
                                String machineName = "Machine 1";

                                VendingMachine vendingMachine = new VendingMachine();
                                vendingMachine.setId(UUID.randomUUID());
                                vendingMachine.setUser(user);
                                vendingMachine.setName(machineName);

                                when(vendingMachineRepository.findByNameAndUserId(machineName, user.getId()))
                                        .thenReturn(Optional.of(vendingMachine));

                                VendingMachine result = vendingMachineService.findVendingMachineByNameAndUserId(machineName, user);

                                assertEquals(result,vendingMachine);
                                verify(vendingMachineRepository).findByNameAndUserId(machineName, user.getId());
                        }
                }

                @Nested
                @DisplayName("Failure Cases")
                class FailureCases {

                        @Test
                        @DisplayName("findVendingMachineByNameAndUserId - not found")
                        void testFindVendingMachineByNameAndUserId_NotFound_ShouldThrowResourceNotFoundException() {
                                User user = new User();
                                user.setId(UUID.randomUUID());
                                String machineName = "Nonexistent Machine";

                                when(vendingMachineRepository.findByNameAndUserId(machineName, user.getId()))
                                        .thenReturn(Optional.empty());

                                assertThrows(ResourceNotFoundException.class, () -> {
                                        vendingMachineService.findVendingMachineByNameAndUserId(machineName, user);
                                });

                                verify(vendingMachineRepository).findByNameAndUserId(machineName, user.getId());
                        }
                }
        }
}
