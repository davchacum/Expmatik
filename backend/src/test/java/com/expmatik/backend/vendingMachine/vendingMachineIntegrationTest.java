package com.expmatik.backend.vendingMachine;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.expmatik.backend.vendingMachine.DTOs.VendingMachineCreate;
import com.expmatik.backend.vendingMachine.DTOs.VendingMachineUpdate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import jakarta.transaction.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class vendingMachineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // == Test Get /api/vending-machines/{id} ==

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testGetVendingMachineById_Success_shouldReturnVendingMachine() throws Exception {
        UUID vendingMachineId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(get("/api/vending-machines/{id}", vendingMachineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vendingMachineId.toString()))
                .andExpect(jsonPath("$.name").value("Máquina 1"))
                .andExpect(jsonPath("$.location").value("Edificio A, Planta Baja"))
                .andExpect(jsonPath("$.rowCount").value(1))
                .andExpect(jsonPath("$.columnCount").value(1));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void getVendingMachineById_whenNotExists_shouldReturn404() throws Exception {
        UUID vendingMachineId = UUID.fromString("00000000-0000-0000-0000-000000000099");

        mockMvc.perform(get("/api/vending-machines/{id}", vendingMachineId))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithUserDetails("admin2@expmatik.com")
    void getVendingMachineById_whenUserIsNotOwner_shouldReturn403() throws Exception {
        UUID vendingMachineId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(get("/api/vending-machines/{id}", vendingMachineId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("repo@expmatik.com")
    void getVendingMachineById_whenUserHasNoPermissionRole_shouldReturn403() throws Exception {
        UUID vendingMachineId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(get("/api/vending-machines/{id}", vendingMachineId))
                .andExpect(status().isForbidden());
    }

    // == Test Get /api/vending-machines ==

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testGetAllVendingMachines_Success_shouldReturnVendingMachinesList() throws Exception {
        mockMvc.perform(get("/api/vending-machines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value("00000000-0000-0000-0000-000000000001"))
                .andExpect(jsonPath("$.content[0].name").value("Máquina 1"))
                .andExpect(jsonPath("$.content[0].location").value("Edificio A, Planta Baja"))
                .andExpect(jsonPath("$.content[0].rowCount").value(1))
                .andExpect(jsonPath("$.content[0].columnCount").value(1));
    }

    @Test
    @WithUserDetails("admin2@expmatik.com")
    void testGetAllVendingMachines_NoResults_shouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/vending-machines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    @WithUserDetails("repo@expmatik.com")
    void testGetAllVendingMachines_UserHasNoPermissionRole_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/vending-machines"))
                .andExpect(status().isForbidden());
    }

    // == Post /api/vending-machines ==

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateVendingMachine_Success_shouldReturnCreatedVendingMachine() throws Exception {
        VendingMachineCreate request = new VendingMachineCreate(
            "Edificio B, Planta Alta",
            "Máquina Nueva",
            3,
            2,
            10
        );

        String requestBody = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/vending-machines")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Máquina Nueva"))
                .andExpect(jsonPath("$.location").value("Edificio B, Planta Alta"))
                .andExpect(jsonPath("$.rowCount").value(2))
                .andExpect(jsonPath("$.columnCount").value(3))
                .andReturn();
        String json = result.getResponse().getContentAsString();
        UUID newVendingMachineId =UUID.fromString((JsonPath.read(json,"$.id")));

        mockMvc.perform(get("/api/vending-slots/"+newVendingMachineId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testCreateVendingMachine_DuplicateName_shouldReturn409() throws Exception {
        VendingMachineCreate request = new VendingMachineCreate(
            "Edificio A, Planta Baja",
            "Máquina 1",
            3,
            2,
            10
        );

        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/vending-machines")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isConflict());
    }

    @Test
    @WithUserDetails("repo@expmatik.com")
    void testCreateVendingMachine_UserHasNoPermissionRole_shouldReturn403() throws Exception {
        VendingMachineCreate request = new VendingMachineCreate(
            "Edificio B, Planta Alta",
            "Máquina 2",
            3,
            2,
            10
        );

        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/vending-machines")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    // == Test Put /api/vending-machines/{id} ==

     @Test
     @WithUserDetails("admin@expmatik.com")
     void testUpdateVendingMachine_Success_shouldReturnUpdatedVendingMachine() throws Exception {
        UUID vendingMachineId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        VendingMachineUpdate request = new VendingMachineUpdate(
            "Edificio A, Planta Baja",
            "Máquina 1 Actualizada"
        );

        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/vending-machines/{id}", vendingMachineId)
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vendingMachineId.toString()))
                .andExpect(jsonPath("$.name").value("Máquina 1 Actualizada"))
                .andExpect(jsonPath("$.location").value("Edificio A, Planta Baja"))
                .andExpect(jsonPath("$.rowCount").value(1))
                .andExpect(jsonPath("$.columnCount").value(1));
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testUpdateVendingMachine_DuplicateName_shouldReturn409() throws Exception {
        UUID vendingMachineId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        VendingMachineUpdate request = new VendingMachineUpdate(
            "Edificio A, Planta Baja",
            "Máquina 2"
        );

        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/vending-machines/{id}", vendingMachineId)
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isConflict());
    }

    @Test
    @WithUserDetails("admin@expmatik.com")
    void testUpdateVendingMachine_NotFound_shouldReturn404() throws Exception {
        UUID vendingMachineId = UUID.fromString("00000000-0000-0000-0000-000000000099");

        VendingMachineUpdate request = new VendingMachineUpdate(
            "Edificio A, Planta Baja",
            "Máquina 1 Actualizada"
        );

        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/vending-machines/{id}", vendingMachineId)
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("admin2@expmatik.com")
    void testUpdateVendingMachine_UserIsNotOwner_shouldReturn403() throws Exception {
        UUID vendingMachineId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        VendingMachineUpdate request = new VendingMachineUpdate(
            "Edificio A, Planta Baja",
            "Máquina 1 Actualizada"
        );

        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/vending-machines/{id}", vendingMachineId)
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("repo@expmatik.com")
    void testUpdateVendingMachine_UserHasNoPermissionRole_shouldReturn403() throws Exception {
        UUID vendingMachineId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        VendingMachineUpdate request = new VendingMachineUpdate(
            "Edificio A, Planta Baja",
            "Máquina 1 Actualizada"
        );
        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/vending-machines/{id}", vendingMachineId)
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

}