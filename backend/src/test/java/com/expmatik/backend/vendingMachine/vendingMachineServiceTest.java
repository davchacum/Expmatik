package com.expmatik.backend.vendingMachine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
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
public class vendingMachineServiceTest {

    @Mock
    private VendingMachineRepository vendingMachineRepository;

    @Mock
    private VendingSlotService vendingSlotService;

    @Spy
    @InjectMocks
    private VendingMachineService vendingMachineService;

    // == Test cases for createVendingMachine ==

    @Test
    @DisplayName("createVendingMachine - success")
    void testCreateVendingMachineSuccess() {
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

        assertThat(result.getName()).isEqualTo("Machine 1");
        assertThat(result.getUser()).isEqualTo(user);

        verify(vendingMachineRepository).save(any(VendingMachine.class));
        verify(vendingMachineRepository).save(argThat(vm ->
            vm.getUser().equals(user) &&
            vm.getName().equals("Machine 1")
        ));
        verify(vendingSlotService).createVendingSlotsForMachine(eq(result), eq(rowCount), eq(columnCount), eq(maxCapacityPerSlot));
    }

    @Test
    @DisplayName("createVendingMachine - name conflict")
    void testCreateVendingMachineNameConflict() {
        User user = new User();
        user.setId(UUID.randomUUID());

        VendingMachineCreate dto = new VendingMachineCreate(
                "Random","Machine 1", 3, 4, 10
        );

        when(vendingMachineRepository.findByNameAndUserId("Machine 1", user.getId()))
                .thenReturn(Optional.of(new VendingMachine()));

        try {
            vendingMachineService.createVendingMachine(dto, user);
        } catch (Exception ex) {
            assertThat(ex).isInstanceOf(ConflictException.class);
            assertThat(ex.getMessage()).isEqualTo("A vending machine with the same name already exists.");
        }

        verify(vendingMachineRepository).findByNameAndUserId("Machine 1", user.getId());
        verify(vendingMachineRepository).findByNameAndUserId(eq("Machine 1"), eq(user.getId()));
    }

    // == Test cases for getVendingMachineById ==

    @Test
    @DisplayName("getVendingMachineById - success")
    void testGetVendingMachineByIdSuccess() {
        User user = new User();
        user.setId(UUID.randomUUID());

        VendingMachine vendingMachine = new VendingMachine();
        vendingMachine.setId(UUID.randomUUID());
        vendingMachine.setUser(user);

        when(vendingMachineRepository.findById(vendingMachine.getId()))
                .thenReturn(Optional.of(vendingMachine));

        VendingMachine result = vendingMachineService.getVendingMachineById(vendingMachine.getId(), user);

        assertThat(result).isEqualTo(vendingMachine);
        verify(vendingMachineRepository).findById(vendingMachine.getId());
    }

    @Test
    @DisplayName("getVendingMachineById - not found")
    void testGetVendingMachineByIdNotFound() {
        User user = new User();
        user.setId(UUID.randomUUID());
        UUID vendingMachineId = UUID.randomUUID();

        when(vendingMachineRepository.findById(vendingMachineId))
                .thenReturn(Optional.empty());

        try {
            vendingMachineService.getVendingMachineById(vendingMachineId, user);
        } catch (Exception ex) {
            assertThat(ex).isInstanceOf(ResourceNotFoundException.class);
            assertThat(ex.getMessage()).isEqualTo("The vending machine does not exist.");
        }

        verify(vendingMachineRepository).findById(vendingMachineId);
    }

    @Test
    @DisplayName("getVendingMachineById - access denied")
    void testGetVendingMachineByIdAccessDenied() {
        User user = new User();
        user.setId(UUID.randomUUID());

        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());

        VendingMachine vendingMachine = new VendingMachine();
        vendingMachine.setId(UUID.randomUUID());
        vendingMachine.setUser(otherUser);

        when(vendingMachineRepository.findById(vendingMachine.getId()))
                .thenReturn(Optional.of(vendingMachine));

        try {
            vendingMachineService.getVendingMachineById(vendingMachine.getId(), user);
        } catch (Exception ex) {
            assertThat(ex).isInstanceOf(AccessDeniedException.class);
            assertThat(ex.getMessage()).isEqualTo("The user is not the owner of the vending machine.");
        }

        verify(vendingMachineRepository).findById(vendingMachine.getId());
    }

    // == Test cases for updateVendingMachine ==

    // @Transactional
    // public VendingMachine updateVendingMachine(UUID vendingMachineId, VendingMachineUpdate vendingMachineUpdate, User user) {
    //     VendingMachine vendingMachine = findVendingMachineById(vendingMachineId);
    //     validateVendingMachineOwnership(vendingMachine, user);
    //     validateVendingMachineNameUniqueness(vendingMachineUpdate.name(), user);
    //     vendingMachine.setLocation(vendingMachineUpdate.location());
    //     vendingMachine.setName(vendingMachineUpdate.name());
    //     return vendingMachineRepository.save(vendingMachine);
    // }

    @Test
    @DisplayName("updateVendingMachine - success")
    void testUpdateVendingMachineSuccess() {
        User user = new User();
        user.setId(UUID.randomUUID());

        VendingMachine vendingMachine = new VendingMachine();
        vendingMachine.setId(UUID.randomUUID());
        vendingMachine.setUser(user);
        vendingMachine.setName("Old Name");
        vendingMachine.setLocation("Old Location");

        VendingMachineUpdate dto = new VendingMachineUpdate(
                "New Location", "New Name"
        );

        when(vendingMachineRepository.findById(vendingMachine.getId()))
                .thenReturn(Optional.of(vendingMachine));

        when(vendingMachineRepository.findByNameAndUserId("New Name", user.getId()))
                .thenReturn(Optional.empty());

        when(vendingMachineRepository.save(any(VendingMachine.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VendingMachine result = vendingMachineService.updateVendingMachine(vendingMachine.getId(), dto, user);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getLocation()).isEqualTo("New Location");

        verify(vendingMachineRepository).findById(vendingMachine.getId());
        verify(vendingMachineRepository).findByNameAndUserId("New Name", user.getId());
        verify(vendingMachineRepository).save(any(VendingMachine.class));
    }

    @Test
    @DisplayName("updateVendingMachine - name conflict")
    void testUpdateVendingMachineNameConflict() {
        User user = new User();
        user.setId(UUID.randomUUID());

        VendingMachine vendingMachine = new VendingMachine();
        vendingMachine.setId(UUID.randomUUID());
        vendingMachine.setUser(user);
        vendingMachine.setName("Old Name");

        VendingMachineUpdate dto = new VendingMachineUpdate(
                "New Location", "Existing Name"
        );

        when(vendingMachineRepository.findById(vendingMachine.getId()))
                .thenReturn(Optional.of(vendingMachine));

        when(vendingMachineRepository.findByNameAndUserId("Existing Name", user.getId()))
                .thenReturn(Optional.of(new VendingMachine()));

        try {
            vendingMachineService.updateVendingMachine(vendingMachine.getId(), dto, user);
        } catch (Exception ex) {
            assertThat(ex).isInstanceOf(ConflictException.class);
            assertThat(ex.getMessage()).isEqualTo("A vending machine with the same name already exists.");
        }

        verify(vendingMachineRepository).findById(vendingMachine.getId());
        verify(vendingMachineRepository).findByNameAndUserId("Existing Name", user.getId());
    }

    @Test
    @DisplayName("updateVendingMachine - not found")
    void testUpdateVendingMachineNotFound() {
        User user = new User();
        user.setId(UUID.randomUUID());
        UUID vendingMachineId = UUID.randomUUID();

        VendingMachineUpdate dto = new VendingMachineUpdate(
                "New Location", "New Name"
        );

        when(vendingMachineRepository.findById(vendingMachineId))
                .thenReturn(Optional.empty());

        try {
            vendingMachineService.updateVendingMachine(vendingMachineId, dto, user);
        } catch (Exception ex) {
            assertThat(ex).isInstanceOf(ResourceNotFoundException.class);
            assertThat(ex.getMessage()).isEqualTo("The vending machine does not exist.");
        }

        verify(vendingMachineRepository).findById(vendingMachineId);
    }
}
