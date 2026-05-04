package com.expmatik.backend.maintenance;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.testSecurityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.expmatik.backend.maintenance.DTOs.MaintenanceCreate;
import com.expmatik.backend.maintenance.DTOs.MaintenanceUpdate;
import com.expmatik.backend.maintenanceDetail.DTOs.MaintenanceDetailCreate;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class MaintenanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserDetailsService userDetailsService;

    @Nested
    @DisplayName("GET /api/maintenances/{id}")
    class GetMaintenanceById {
        
        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("Should return maintenance when maintenance exists and administrator has access")
            @WithUserDetails("admin@expmatik.com")
            void testGetMaintenanceById_ValidIdAndAdministratorHasAccess_shouldReturnMaintenance() throws Exception {

                UUID maintenanceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(get("/api/maintenances/{id}", maintenanceId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(maintenanceId.toString()))
                    .andExpect(jsonPath("$.description").value("Mantenimiento completado"))
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.maintainer.email").value("repo@expmatik.com"))
                    .andExpect(jsonPath("$.administrator.email").value("admin@expmatik.com"))
                    .andExpect(jsonPath("$.vendingMachine.id").value("00000000-0000-0000-0000-000000000002"));
            }

            @Test
            @DisplayName("Should return maintenance when maintenance exists and maintainer has access")
            @WithUserDetails("repo@expmatik.com")
            void testGetMaintenanceById_ValidIdAndMaintainerHasAccess_shouldReturnMaintenance() throws Exception {

                UUID maintenanceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(get("/api/maintenances/{id}", maintenanceId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(maintenanceId.toString()))
                    .andExpect(jsonPath("$.description").value("Mantenimiento completado"))
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.maintainer.email").value("repo@expmatik.com"))
                    .andExpect(jsonPath("$.administrator.email").value("admin@expmatik.com"))
                    .andExpect(jsonPath("$.vendingMachine.id").value("00000000-0000-0000-0000-000000000002"));
            }

        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("Should return 404 when maintenance does not exist")
            @WithUserDetails("admin@expmatik.com")
            void testGetMaintenanceById_NonExistingId_shouldReturn404() throws Exception {

                UUID nonExistingId = UUID.fromString("00000000-0000-0000-0000-000000000999");

                mockMvc.perform(get("/api/maintenances/{id}", nonExistingId))
                    .andExpect(status().isNotFound());
            }

            @Test
            @DisplayName("Should return 403 when administrator does not have access to maintenance")
            @WithUserDetails("admin2@expmatik.com")
            void testGetMaintenanceById_UnauthorizedAdministrator_shouldReturn403() throws Exception {

                UUID maintenanceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(get("/api/maintenances/{id}", maintenanceId))
                    .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("Should return 403 when maintainer has access but maintenance is in DRAFT status")
            @WithUserDetails("repo@expmatik.com")
            void testGetMaintenanceById_ValidIdAndMaintainerHasAccessButMaintenanceIsInDraftStatus_shouldReturn403() throws Exception {

                UUID maintenanceId = UUID.fromString("00000000-0000-0000-0000-000000000003");

                mockMvc.perform(get("/api/maintenances/{id}", maintenanceId))
                    .andExpect(status().isForbidden());
            }

        }

    }

    @Nested
    @DisplayName("POST /api/maintenances")
    class CreateMaintenance {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("Should create maintenance when request is valid")
            @WithUserDetails("admin@expmatik.com")
            void testCreateMaintenance_ValidRequest_shouldCreateMaintenance() throws Exception {

                MaintenanceCreate maintenanceCreate = new MaintenanceCreate(
                    LocalDate.of(2050, 12, 31),
                    "Test maintenance",
                    "repo@expmatik.com",
                    "Máquina 1"
                );

                mockMvc.perform(MockMvcRequestBuilders.post("/api/maintenances")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(maintenanceCreate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.description").value("Test maintenance"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.maintenanceDate").value("2050-12-31"))
                .andExpect(jsonPath("$.vendingMachine.name").value("Máquina 1"))
                .andExpect(jsonPath("$.maintainer.email").value("repo@expmatik.com"))
                .andExpect(jsonPath("$.administrator.email").value("admin@expmatik.com"));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("Should return 404 when machine name does not exist")
            @WithUserDetails("admin@expmatik.com")
            void testCreateMaintenance_InvalidRequest_shouldReturn400() throws Exception {

                MaintenanceCreate maintenanceCreate = new MaintenanceCreate(
                    null,
                    "Test maintenance",
                    "invalid-email",
                    "NotExistingMachine"
                );

                mockMvc.perform(MockMvcRequestBuilders.post("/api/maintenances")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(maintenanceCreate)))
                .andExpect(status().isNotFound());
            }

            @Test
            @DisplayName("Should return 404 when maintainer email does not exist")
            @WithUserDetails("admin@expmatik.com")
            void testCreateMaintenance_InvalidMaintainerEmail_shouldReturn404() throws Exception {

                MaintenanceCreate maintenanceCreate = new MaintenanceCreate(
                    LocalDate.of(2050, 12, 31),
                    "Test maintenance",
                    "nonexistent@example.com",
                    "Máquina 1"
                );

                mockMvc.perform(MockMvcRequestBuilders.post("/api/maintenances")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(maintenanceCreate)))
                .andExpect(status().isNotFound());
            }

            @Test
            @DisplayName("Should return 400 when maintainer email belongs to an administrator")
            @WithUserDetails("admin@expmatik.com")
            void testCreateMaintenance_AdministratorEmail_shouldReturn400() throws Exception {

                MaintenanceCreate maintenanceCreate = new MaintenanceCreate(
                    LocalDate.of(2050, 12, 31),
                    "Test maintenance",
                    "admin2@expmatik.com",
                    "Máquina 1"
                );

                mockMvc.perform(MockMvcRequestBuilders.post("/api/maintenances")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(maintenanceCreate)))
                .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("Should return 403 when user is not an administrator")
            @WithUserDetails("repo@expmatik.com")
            void testCreateMaintenance_UserIsNotAdministrator_shouldReturn403() throws Exception {

                MaintenanceCreate maintenanceCreate = new MaintenanceCreate(
                    LocalDate.of(2050, 12, 31),
                    "Test maintenance",
                    "repo@expmatik.com",
                    "Máquina 1"
                );
                mockMvc.perform(MockMvcRequestBuilders.post("/api/maintenances")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(maintenanceCreate)))
                .andExpect(status().isForbidden());
            }
        }
    }

    @Nested
    @DisplayName("PUT /api/maintenances/{id}")
    class UpdateMaintenance {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("Should update maintenance when request is valid and user is administrator")
            @WithUserDetails("admin@expmatik.com")
            void testUpdateMaintenance_ValidRequest_shouldReturn200() throws Exception {

                MaintenanceUpdate maintenanceUpdate = new MaintenanceUpdate(
                    "Updated description",
                    "repo@expmatik.com"
                );

                mockMvc.perform(MockMvcRequestBuilders.put("/api/maintenances/{id}", "00000000-0000-0000-0000-000000000003")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(maintenanceUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000003"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.maintainer.email").value("repo@expmatik.com"))
                .andExpect(jsonPath("$.vendingMachine.name").value("Máquina 2"))
                .andExpect(jsonPath("$.maintenanceDate").value("2050-03-15"));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("Should return 404 when maintenance does not exist")
            @WithUserDetails("admin@expmatik.com")
            void testUpdateMaintenance_NonExistingId_shouldReturn404() throws Exception {

                MaintenanceUpdate maintenanceUpdate = new MaintenanceUpdate(
                    "Updated description",
                    "repo@expmatik.com"
                );

                mockMvc.perform(MockMvcRequestBuilders.put("/api/maintenances/{id}", "00000000-0000-0000-0000-000000000999")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(maintenanceUpdate)))
                .andExpect(status().isNotFound());

            }

            @Test
            @DisplayName("Should return 404 when maintainer does not exist")
            @WithUserDetails("admin@expmatik.com")
            void testUpdateMaintenance_NonExistingMaintainer_shouldReturn404() throws Exception {

                MaintenanceUpdate maintenanceUpdate = new MaintenanceUpdate(
                    "Updated description",
                    "repo@example.com"
                );

                mockMvc.perform(MockMvcRequestBuilders.put("/api/maintenances/{id}", "00000000-0000-0000-0000-00000000003")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(maintenanceUpdate)))
                .andExpect(status().isNotFound());

            }

            @Test
            @DisplayName("Should return 400 when maintenance not in DRAFT status")
            @WithUserDetails("admin@expmatik.com")
            void testUpdateMaintenance_NotInDraftStatus_shouldReturn400() throws Exception {

                MaintenanceUpdate maintenanceUpdate = new MaintenanceUpdate(
                    "Updated description",
                    "repo@expmatik.com"
                );

                mockMvc.perform(MockMvcRequestBuilders.put("/api/maintenances/{id}", "00000000-0000-0000-0000-00000000001")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(maintenanceUpdate)))
                .andExpect(status().isBadRequest());

            }

            @Test
            @DisplayName("Should return 400 when maintainer email belongs to an administrator")
            @WithUserDetails("admin@expmatik.com")
            void testUpdateMaintenance_AdministratorEmail_shouldReturn400() throws Exception {

                MaintenanceUpdate maintenanceUpdate = new MaintenanceUpdate(
                    "Updated description",
                    "admin2@expmatik.com"
                );

                mockMvc.perform(MockMvcRequestBuilders.put("/api/maintenances/{id}", "00000000-0000-0000-0000-00000000003")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(maintenanceUpdate)))
                .andExpect(status().isBadRequest());

            }

            @Test
            @DisplayName("Should return 403 when user is not an administrator")
            @WithUserDetails("repo@expmatik.com")
            void testUpdateMaintenance_UserIsNotAdministrator_shouldReturn403() throws Exception {

                MaintenanceUpdate maintenanceUpdate = new MaintenanceUpdate(
                    "Updated description",
                    "repo@expmatik.com"
                );

                mockMvc.perform(MockMvcRequestBuilders.put("/api/maintenances/{id}", "00000000-0000-0000-0000-000000000999")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(maintenanceUpdate)))
                .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("Should return 403 when user is an administrator but does not have access to the maintenance")
            @WithUserDetails("admin2@expmatik.com")
            void testUpdateMaintenance_AdministratorWithoutAccess_shouldReturn403() throws Exception {

                MaintenanceUpdate maintenanceUpdate = new MaintenanceUpdate(
                    "Updated description",
                    "repo@expmatik.com"
                );

                mockMvc.perform(MockMvcRequestBuilders.put("/api/maintenances/{id}", "00000000-0000-0000-0000-000000000003")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(maintenanceUpdate)))
                .andExpect(status().isForbidden());
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/maintenances/{id}")
    class DeleteMaintenance {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("Should delete maintenance when maintenance is in DRAFT status and user is administrator")
            @WithUserDetails("admin@expmatik.com")
            void testDeleteMaintenance_InDraftStatus_shouldDeleteMaintenance() throws Exception {

                UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(MockMvcRequestBuilders.delete("/api/maintenances/{id}", "00000000-0000-0000-0000-00000000003")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isNoContent());

                mockMvc.perform(get("/api/product-info/get-or-create-product/" + productId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.productId").value(productId.toString()))
                                .andExpect(jsonPath("$.stockQuantity").value(201));

                
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("Should return 400 when trying to delete maintenance not in DRAFT status")
            @WithUserDetails("admin@expmatik.com")
            void testDeleteMaintenance_NotInDraftStatus_shouldReturn400() throws Exception {

                mockMvc.perform(MockMvcRequestBuilders.delete("/api/maintenances/{id}", "00000000-0000-0000-0000-00000000002")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isBadRequest());
            }
        }

        @Test
        @DisplayName("Should return 403 when user is not an administrator")
        @WithUserDetails("repo@expmatik.com")
        void testDeleteMaintenance_UserIsNotAdministrator_shouldReturn403() throws Exception {

            mockMvc.perform(MockMvcRequestBuilders.delete("/api/maintenances/{id}", "00000000-0000-0000-0000-00000000003")
                                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/maintenances")
    class SearchMaintenances {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {
            
            @Test
            @DisplayName("Should return maintenances when user is administrator and there are maintenances in the system")
            @WithUserDetails("admin@expmatik.com")
            void testSearchMaintenances_Administrator_shouldReturnMaintenances() throws Exception {

                mockMvc.perform(MockMvcRequestBuilders.get("/api/maintenances")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content.length()").value(10));
            }

            @Test
            @DisplayName("Should return maintenances when user is administrator and there are maintenances in the system even if machineName filter is blank")
            @WithUserDetails("admin@expmatik.com")
            void testSearchMaintenances_Administrator_BlankMachineName() throws Exception {

                mockMvc.perform(MockMvcRequestBuilders.get("/api/maintenances")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .param("machineName", ""))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content.length()").value(10));
            }

            @Test
            @DisplayName("Should return maintenances filtered by machine name when user is administrator")
            @WithUserDetails("admin@expmatik.com")
            void testSearchMaintenances_Administrator_FilterByMachineName() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.get("/api/maintenances")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .param("machineName", "Máquina 2"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content.length()").value(7));
            }

            @Test
            @DisplayName("Should return maintenances when user is maintainer and there are maintenances in the system")
            @WithUserDetails("repo@expmatik.com")
            void testSearchMaintenances_Maintainer_shouldReturnMaintenances() throws Exception {

                mockMvc.perform(MockMvcRequestBuilders.get("/api/maintenances")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content.length()").value(7));
            }

            @Test
            @DisplayName("Should return empty list when user is administrator and there are no maintenances in the system")
            @WithUserDetails("admin2@expmatik.com")
            void testSearchMaintenances_AdministratorNoResults_shouldReturnEmptyList() throws Exception {

                mockMvc.perform(MockMvcRequestBuilders.get("/api/maintenances")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content.length()").value(0));
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/maintenances/{id}/pending")
    class PendingMaintenance {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("Should update maintenance status from DRAFT to PENDING when user is administrator and maintenance has maintenance details with Product Perishable")
            @WithUserDetails("admin@expmatik.com")
            void testPendingMaintenance_FromDraftToPendingWithPerishableProducts_shouldUpdateStatus() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/pending", "00000000-0000-0000-0000-000000000003")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000003"))
                        .andExpect(jsonPath("$.status").value("PENDING"));

                UserDetails repoUser = userDetailsService.loadUserByUsername("repo@expmatik.com");
                TestSecurityContextHolder.setAuthentication(
                    new UsernamePasswordAuthenticationToken(repoUser, repoUser.getPassword(), repoUser.getAuthorities()));

                mockMvc.perform(get("/api/notifications")
                                .with(testSecurityContext())
                                        .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].type").value("ASSIGNED_RESTOCKING"))
                                .andExpect(jsonPath("$.content[0].isRead").value(false))
                                .andExpect(jsonPath("$.content[0].createdAt").value(Matchers.startsWith(LocalDate.now().toString())));
            }

            @Test
            @DisplayName("Should update maintenance status from DRAFT to PENDING when user is administrator and maintenance does not have maintenance details with Product Perishable")
            @WithUserDetails("admin@expmatik.com")
            void testPendingMaintenance_FromDraftToPendingWithoutPerishableProducts_shouldUpdateStatus() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/pending", "00000000-0000-0000-0000-000000000004")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000004"))
                        .andExpect(jsonPath("$.status").value("PENDING"));

                                UserDetails repoUser = userDetailsService.loadUserByUsername("repo@expmatik.com");
                TestSecurityContextHolder.setAuthentication(
                    new UsernamePasswordAuthenticationToken(repoUser, repoUser.getPassword(), repoUser.getAuthorities()));

                mockMvc.perform(get("/api/notifications")
                                .with(testSecurityContext())
                                        .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].type").value("ASSIGNED_RESTOCKING"))
                                .andExpect(jsonPath("$.content[0].isRead").value(false))
                                .andExpect(jsonPath("$.content[0].createdAt").value(Matchers.startsWith(LocalDate.now().toString())));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("Should return 403 when user is an administrator but does not have access to the maintenance")
            @WithUserDetails("admin2@expmatik.com")
            void testPendingMaintenance_ValidIdButUserDoesNotHaveAccess_shouldReturn403() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/pending", "00000000-0000-0000-0000-000000000004")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("Should return 403 when user is not an administrator")
            @WithUserDetails("repo@expmatik.com")
            void testPendingMaintenance_NotAdministrator_shouldReturn403() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/pending", "00000000-0000-0000-0000-000000000004")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("Should return 400 when trying to update maintenance status to PENDING but maintenance does not have maintenance details")
            @WithUserDetails("admin@expmatik.com")
            void testPendingMaintenance_ValidIdButEmptyDetails_shouldReturn400() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/pending", "00000000-0000-0000-0000-000000000005")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("Cannot change status to PENDING without any maintenance details. Please add at least one maintenance detail before changing the status to PENDING."));
            }

            @Test
            @DisplayName("Should return 400 when trying to update maintenance status to PENDING but maintenance is already in PENDING status")
            @WithUserDetails("admin@expmatik.com")
            void testPendingMaintenance_ValidIdButAlreadyPending_shouldReturn400() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/pending", "00000000-0000-0000-0000-000000000002")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("Maintenance status must be DRAFT to be set as PENDING."));
            }

            @Test
            @DisplayName("Should return 400 when trying to update maintenance status to PENDING but maintenance is in DELAYED status")
            @WithUserDetails("admin@expmatik.com")
            void testPendingMaintenance_ValidIdButDelayed_shouldReturn400() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/pending", "00000000-0000-0000-0000-000000000007")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("Maintenance status must be DRAFT to be set as PENDING."));
            }

            @Test
            @DisplayName("Should return 400 when trying to update maintenance status to PENDING but maintenance is in COMPLETED status")
            @WithUserDetails("admin@expmatik.com")
            void testPendingMaintenance_ValidIdButCompleted_shouldReturn400() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/pending", "00000000-0000-0000-0000-000000000006")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("Maintenance status must be DRAFT to be set as PENDING."));
            }

            @Test
            @DisplayName("Should return 400 when trying to update maintenance status to PENDING but maintenance is in REJECTED_EXPIRED status")
            @WithUserDetails("admin@expmatik.com")
            void testPendingMaintenance_ValidIdButRejectedExpired_shouldReturn400() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/pending", "00000000-0000-0000-0000-000000000008")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("Maintenance status must be DRAFT to be set as PENDING."));
            }

        }

    }

    @Nested
    @DisplayName("PATCH /api/maintenances/{id}/completed")
    class CompletedMaintenance {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases { 

            @Test
            @DisplayName("Should update maintenance status from PENDING to COMPLETED when user is maintainer")
            @WithUserDetails("repo@expmatik.com")
            void testCompletedMaintenance_FromPendingToCompleted_shouldUpdateStatus() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/completed", "00000000-0000-0000-0000-000000000002")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000002"))
                        .andExpect(jsonPath("$.status").value("COMPLETED"));

                //Como administrador ahora comprobar que aumente el stock de la ranura

                UserDetails adminUser = userDetailsService.loadUserByUsername("admin@expmatik.com");
                TestSecurityContextHolder.setAuthentication(
                    new UsernamePasswordAuthenticationToken(adminUser, adminUser.getPassword(), adminUser.getAuthorities()));

                mockMvc.perform(get("/api/vending-slots/{id}/expiration-batches", "00000000-0000-0000-0000-000000000002")
                            .with(testSecurityContext())
                                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(1))
                        .andExpect(jsonPath("$[0].expirationDate").value("2050-12-31"))
                        .andExpect(jsonPath("$[0].vendingSlot.currentStock").value("1"));

                mockMvc.perform(get("/api/notifications")
                                .with(testSecurityContext())
                                        .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].type").value("COMPLETED_RESTOCKING"))
                                .andExpect(jsonPath("$.content[0].isRead").value(false))
                                .andExpect(jsonPath("$.content[0].createdAt").value(Matchers.startsWith(LocalDate.now().toString())));
            }

            @Test
            @DisplayName("Should update maintenance status from PENDING to COMPLETED when user is maintainer with non perishable products")
            @WithUserDetails("repo@expmatik.com")
            void testCompletedMaintenance_FromPendingToCompleted_NonPerishableProducts_shouldUpdateStatus() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/completed", "00000000-0000-0000-0000-000000000009")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000009"))
                        .andExpect(jsonPath("$.status").value("COMPLETED"));

                //Como administrador ahora comprobar que aumente el stock de la ranura

                UserDetails adminUser = userDetailsService.loadUserByUsername("admin@expmatik.com");
                TestSecurityContextHolder.setAuthentication(
                    new UsernamePasswordAuthenticationToken(adminUser, adminUser.getPassword(), adminUser.getAuthorities()));

                mockMvc.perform(get("/api/vending-slots/{id}/expiration-batches", "00000000-0000-0000-0000-000000000009")
                            .with(testSecurityContext())
                                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(1))
                        .andExpect(jsonPath("$[0].expirationDate").value((Object) null))
                        .andExpect(jsonPath("$[0].vendingSlot.currentStock").value("2"));

                mockMvc.perform(get("/api/notifications")
                                .with(testSecurityContext())
                                        .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].type").value("COMPLETED_RESTOCKING"))
                                .andExpect(jsonPath("$.content[0].isRead").value(false))
                                .andExpect(jsonPath("$.content[0].createdAt").value(Matchers.startsWith(LocalDate.now().toString())));
            }

            @Test
            @DisplayName("Should update maintenance status from DELAYED to COMPLETED when user is maintainer")
            @WithUserDetails("repo@expmatik.com")
            void testCompletedMaintenance_FromDelayedToCompleted_shouldUpdateStatus() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/completed", "00000000-0000-0000-0000-000000000007")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000007"))
                        .andExpect(jsonPath("$.status").value("COMPLETED"));

                //Como administrador ahora comprobar que aumente el stock de la ranura

                UserDetails adminUser = userDetailsService.loadUserByUsername("admin@expmatik.com");
                TestSecurityContextHolder.setAuthentication(
                    new UsernamePasswordAuthenticationToken(adminUser, adminUser.getPassword(), adminUser.getAuthorities()));

                mockMvc.perform(get("/api/vending-slots/{id}/expiration-batches", "00000000-0000-0000-0000-000000000009")
                            .with(testSecurityContext())
                                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(1))
                        .andExpect(jsonPath("$[0].expirationDate").value((Object) null))
                        .andExpect(jsonPath("$[0].vendingSlot.currentStock").value("1"));

                mockMvc.perform(get("/api/notifications")
                                .with(testSecurityContext())
                                        .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].type").value("COMPLETED_RESTOCKING"))
                                .andExpect(jsonPath("$.content[0].isRead").value(false))
                                .andExpect(jsonPath("$.content[0].createdAt").value(Matchers.startsWith(LocalDate.now().toString())));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("Should return 403 when user is an maintainer but does not have access to the maintenance")
            @WithUserDetails("repo2@expmatik.com")
            void testCompletedMaintenance_ValidIdButUserDoesNotHaveAccess_shouldReturn403() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/completed", "00000000-0000-0000-0000-000000000004")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("Should return 403 when user is not an maintainer")
            @WithUserDetails("admin@expmatik.com")
            void testCompletedMaintenance_NotMaintainer_shouldReturn403() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/completed", "00000000-0000-0000-0000-000000000004")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("Should return 400 when trying to update maintenance status to COMPLETED but maintenance is already in COMPLETED status")
            @WithUserDetails("repo@expmatik.com")
            void testCompletedMaintenance_ValidIdButAlreadyCompleted_shouldReturn400() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/completed", "00000000-0000-0000-0000-000000000001")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("Maintenance status must be PENDING or DELAYED to be set as COMPLETED."));
            }

            @Test
            @DisplayName("Should return 400 when trying to update maintenance status to COMPLETED but maintenance is in REJECTED_EXPIRED status")
            @WithUserDetails("repo@expmatik.com")
            void testCompletedMaintenance_ValidIdButRejectedExpired_shouldReturn400() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/completed", "00000000-0000-0000-0000-000000000008")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("Maintenance status must be PENDING or DELAYED to be set as COMPLETED."));
            }

            @Test
            @DisplayName("Should return 403 when trying to update maintenance status to COMPLETED but maintenance is in DRAFT status and user is maintainer with access to the maintenance")
            @WithUserDetails("repo@expmatik.com")
            void testCompletedMaintenance_ValidIdButDraft_shouldReturn403() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/completed", "00000000-0000-0000-0000-000000000003")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isForbidden());
            }

            @Test
            //404 no existe el mantenimiento
            @DisplayName("Should return 404 when trying to update maintenance does not exist")
            @WithUserDetails("repo@expmatik.com")
            void testCompletedMaintenance_MaintenanceNotFound_shouldReturn404() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/completed", "00000000-0000-0000-0000-000000000099")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isNotFound());
            }

            @Test
            @DisplayName("Should complete successfully and return to inventory when slot overflow or expired products — no error thrown")
            @WithUserDetails("repo@expmatik.com")
            void testCompletedMaintenance_SlotOverflowOrExpiredProduct_shouldReturn200AndCompleteGracefully() throws Exception {
                // maintenance 000000000010: slot maxCapacity=5, currentStock=4, detail qty=2, expiration 2026-03-25 (past)
                // Expected: expired product returned to inventory, completion succeeds
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/completed", "00000000-0000-0000-0000-000000000010")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000010"))
                        .andExpect(jsonPath("$.status").value("COMPLETED"))
                        .andExpect(jsonPath("$.maintenanceDetails[0].quantityRestocked").value(0))
                        .andExpect(jsonPath("$.maintenanceDetails[0].quantityReturned").value(2));
            }
        }
    }

    @Nested
    @DisplayName("POST /api/maintenances/{id}/details")
    class AddMaintenanceDetail {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("Should add maintenance detail to maintenance when request is valid and user is administrator with access to the maintenance")
            @WithUserDetails("admin@expmatik.com")
            void testAddMaintenanceDetail_ValidRequest_shouldAddMaintenanceDetail() throws Exception {

                MaintenanceDetailCreate maintenanceDetailCreate = new MaintenanceDetailCreate(
                        1,
                        LocalDate.of(2050, 5, 15),
                        1,
                        1,
                        "20000001"
                );

                UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(MockMvcRequestBuilders.post("/api/maintenances/{id}/details", "00000000-0000-0000-0000-000000000003")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(maintenanceDetailCreate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.maintenanceDetails.length()").value(2));

                mockMvc.perform(get("/api/product-info/get-or-create-product/" + productId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.productId").value(productId.toString()))
                                .andExpect(jsonPath("$.stockQuantity").value(199));
            }

            @Test
            @DisplayName("Should add maintenance detail to maintenance when request is valid, user is administrator with access to the maintenance and product is not perishable")
            @WithUserDetails("admin@expmatik.com")
            void testAddMaintenanceDetail_ValidRequestAndNotPerishable_shouldAddMaintenanceDetail() throws Exception {

                MaintenanceDetailCreate maintenanceDetailCreate = new MaintenanceDetailCreate(
                        1,
                        null,
                        1,
                        1,
                        "20000000"
                );

                mockMvc.perform(MockMvcRequestBuilders.post("/api/maintenances/{id}/details", "00000000-0000-0000-0000-000000000004")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(maintenanceDetailCreate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.maintenanceDetails.length()").value(2));

                UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000004");

                mockMvc.perform(get("/api/product-info/get-or-create-product/" + productId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.productId").value(productId.toString()))
                                .andExpect(jsonPath("$.stockQuantity").value(99));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("Should return 400 when barcode does not match product in vending slot")
            @WithUserDetails("admin@expmatik.com")
            void testAddMaintenanceDetail_InvalidBarcode_shouldReturn400() throws Exception {

                MaintenanceDetailCreate maintenanceDetailCreate = new MaintenanceDetailCreate(
                        1,
                        null,
                        1,
                        1,
                        "20000001"
                );

                mockMvc.perform(MockMvcRequestBuilders.post("/api/maintenances/{id}/details", "00000000-0000-0000-0000-000000000004")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(maintenanceDetailCreate)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("The barcode does not match the product in the vending slot."));
            }

            @Test
            @DisplayName("Should return 400 when Product expires and Expiration date is not provided")
            @WithUserDetails("admin@expmatik.com")
            void testAddMaintenanceDetail_ExpiringProductWithoutExpirationDate_shouldReturn400() throws Exception {

                MaintenanceDetailCreate maintenanceDetailCreate = new MaintenanceDetailCreate(
                        1,
                        null,
                        1,
                        1,
                        "20000001"
                );

                mockMvc.perform(MockMvcRequestBuilders.post("/api/maintenances/{id}/details", "00000000-0000-0000-0000-000000000003")
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(objectMapper.writeValueAsString(maintenanceDetailCreate)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("Expiration date is required for perishable products."));
            }

            @Test
            @DisplayName("Should return 400 when Product not expires and Expiration date is provided")
            @WithUserDetails("admin@expmatik.com")
            void testAddMaintenanceDetail_NonExpiringProductWithExpirationDate_shouldReturn400() throws Exception {

                MaintenanceDetailCreate maintenanceDetailCreate = new MaintenanceDetailCreate(
                        1,
                        LocalDate.of(2050, 5, 15),
                        1,
                        1,
                        "20000000"
                );

                mockMvc.perform(MockMvcRequestBuilders.post("/api/maintenances/{id}/details", "00000000-0000-0000-0000-000000000004")
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(objectMapper.writeValueAsString(maintenanceDetailCreate)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("Expiration date should not be provided for non-perishable products."));
            }

            @Test
            @DisplayName("Should return 409 when inventory stock is insufficient")
            @WithUserDetails("admin@expmatik.com")
            void testAddMaintenanceDetail_InsufficientInventoryStock_shouldReturn409() throws Exception {

                MaintenanceDetailCreate maintenanceDetailCreate = new MaintenanceDetailCreate(
                        99,
                        null,
                        1,
                        1,
                        "20000000"
                );

                mockMvc.perform(MockMvcRequestBuilders.post("/api/maintenances/{id}/details", "00000000-0000-0000-0000-000000000004")
                                                    .contentType(MediaType.APPLICATION_JSON)
                                                    .content(objectMapper.writeValueAsString(maintenanceDetailCreate)))
                        .andExpect(status().isOk());

                maintenanceDetailCreate = new MaintenanceDetailCreate(
                        100,
                        null,
                        2,
                        1,
                        "20000000"
                );

                mockMvc.perform(MockMvcRequestBuilders.post("/api/maintenances/{id}/details", "00000000-0000-0000-0000-000000000004")
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(objectMapper.writeValueAsString(maintenanceDetailCreate)))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.message").value("Insufficient stock in inventory to create this maintenance detail."));

            }

            @Test
            @DisplayName("Should return 400 when expiration date is before maintenance date")
            @WithUserDetails("admin@expmatik.com")
            void testAddMaintenanceDetail_PastExpirationDate_shouldReturn400() throws Exception {

                MaintenanceDetailCreate maintenanceDetailCreate = new MaintenanceDetailCreate(
                        1,
                        LocalDate.of(2050, 1, 10),
                        1,
                        1,
                        "20000001"
                );

                mockMvc.perform(MockMvcRequestBuilders.post("/api/maintenances/{id}/details", "00000000-0000-0000-0000-000000000003")
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(objectMapper.writeValueAsString(maintenanceDetailCreate)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("The expiration date cannot be before the maintenance date."));
            }

            @Test
            @DisplayName("Should return 403 when user is an administrator but does not have access to the maintenance")
            @WithUserDetails("admin2@expmatik.com")
            void testAddMaintenanceDetail_ForbiddenAccess_shouldReturn403() throws Exception {

                MaintenanceDetailCreate maintenanceDetailCreate = new MaintenanceDetailCreate(
                        1,
                        LocalDate.of(2050, 1, 10),
                        1,
                        1,
                        "20000001"
                );

                mockMvc.perform(MockMvcRequestBuilders.post("/api/maintenances/{id}/details", "00000000-0000-0000-0000-000000000003")
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(objectMapper.writeValueAsString(maintenanceDetailCreate)))
                        .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("Should return 403 when user is maintainer")
            @WithUserDetails("repo@expmatik.com")
            void testAddMaintenanceDetail_UserIsMaintainer_shouldReturn403() throws Exception {

                MaintenanceDetailCreate maintenanceDetailCreate = new MaintenanceDetailCreate(
                        1,
                        LocalDate.of(2050, 1, 10),
                        1,
                        1,
                        "20000001"
                );

                mockMvc.perform(MockMvcRequestBuilders.post("/api/maintenances/{id}/details", "00000000-0000-0000-0000-000000000003")
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(objectMapper.writeValueAsString(maintenanceDetailCreate)))
                        .andExpect(status().isForbidden());
            }


        }
    }

    @Nested
    @DisplayName("PATCH /api/maintenances/{id}/canceled")
    class CancelMaintenance {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("Should cancel maintenance from PENDING and return stock to inventory")
            @WithUserDetails("admin@expmatik.com")
            void testCancelMaintenance_FromPending_shouldCancelAndReturnStock() throws Exception {
                
                // maintenance 000000000002: PENDING, detail qty=1 of Leche Entera (product_info stock=200)
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/canceled", "00000000-0000-0000-0000-000000000002")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000002"))
                        .andExpect(jsonPath("$.status").value("CANCELED"));

                UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000001");
                mockMvc.perform(get("/api/product-info/get-or-create-product/" + productId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.productId").value(productId.toString()))
                        .andExpect(jsonPath("$.stockQuantity").value(201));
            }

            @Test
            @DisplayName("Should cancel maintenance from DELAYED and return stock to inventory")
            @WithUserDetails("admin@expmatik.com")
            void testCancelMaintenance_FromDelayed_shouldCancelAndReturnStock() throws Exception {
                // maintenance 000000000007: DELAYED, detail qty=1 of Leche Entera (product_info stock=200)
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/canceled", "00000000-0000-0000-0000-000000000007")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000007"))
                        .andExpect(jsonPath("$.status").value("CANCELED"));

                UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000001");
                mockMvc.perform(get("/api/product-info/get-or-create-product/" + productId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.stockQuantity").value(201));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("Should return 400 when trying to cancel a DRAFT maintenance")
            @WithUserDetails("admin@expmatik.com")
            void testCancelMaintenance_FromDraft_shouldReturn400() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/canceled", "00000000-0000-0000-0000-000000000003")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("Only maintenance records in PENDING or DELAYED status can be canceled."));
            }

            @Test
            @DisplayName("Should return 400 when trying to cancel a COMPLETED maintenance")
            @WithUserDetails("admin@expmatik.com")
            void testCancelMaintenance_FromCompleted_shouldReturn400() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/canceled", "00000000-0000-0000-0000-000000000001")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("Only maintenance records in PENDING or DELAYED status can be canceled."));
            }

            @Test
            @DisplayName("Should return 400 when trying to cancel a REJECTED_EXPIRED maintenance")
            @WithUserDetails("admin@expmatik.com")
            void testCancelMaintenance_FromRejectedExpired_shouldReturn400() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/canceled", "00000000-0000-0000-0000-000000000008")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("Only maintenance records in PENDING or DELAYED status can be canceled."));
            }

            @Test
            @DisplayName("Should return 403 when user is not an administrator")
            @WithUserDetails("repo@expmatik.com")
            void testCancelMaintenance_NotAdministrator_shouldReturn403() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/canceled", "00000000-0000-0000-0000-000000000002")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("Should return 403 when administrator does not have access to the maintenance")
            @WithUserDetails("admin2@expmatik.com")
            void testCancelMaintenance_AdministratorWithoutAccess_shouldReturn403() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.patch("/api/maintenances/{id}/canceled", "00000000-0000-0000-0000-000000000002")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isForbidden());
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/maintenances/{maintenanceId}/details/{detailId}")
    class DeleteMaintenanceDetail {
        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("Should delete maintenance detail when request is valid and user is administrator with access to the maintenance")
            @WithUserDetails("admin@expmatik.com")
            void testDeleteMaintenanceDetail_ValidRequest_shouldDeleteMaintenanceDetail() throws Exception {

                UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(MockMvcRequestBuilders.delete("/api/maintenances/{maintenanceId}/details/{detailId}", "00000000-0000-0000-0000-000000000003", "00000000-0000-0000-0000-000000000003")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").exists())
                        .andExpect(jsonPath("$.maintenanceDetails.length()").value(0));

                mockMvc.perform(get("/api/product-info/get-or-create-product/" + productId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.productId").value(productId.toString()))
                                .andExpect(jsonPath("$.stockQuantity").value(201));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("should return 404 when maintenance does not exist")
            @WithUserDetails("admin@expmatik.com")
            void testDeleteMaintenanceDetail_MaintenanceNotFound_shouldReturn404() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.delete("/api/maintenances/{maintenanceId}/details/{detailId}", "00000000-0000-0000-0000-000000000099", "00000000-0000-0000-0000-000000000003")
                                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isNotFound());
            }
        }

        @Test
        @DisplayName("should return 404 when maintenance detail does not exist")
        @WithUserDetails("admin@expmatik.com")
        void testDeleteMaintenanceDetail_MaintenanceDetailNotFound_shouldReturn404() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.delete("/api/maintenances/{maintenanceId}/details/{detailId}", "00000000-0000-0000-0000-000000000003", "00000000-0000-0000-0000-000000000099")
                                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when maintenance detail does not belong to the maintenance")
        @WithUserDetails("admin@expmatik.com")
        void testDeleteMaintenanceDetail_MaintenanceDetailNotBelongingToMaintenance_shouldReturn400() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.delete("/api/maintenances/{maintenanceId}/details/{detailId}", "00000000-0000-0000-0000-000000000003", "00000000-0000-0000-0000-000000000002")
                                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 when user is not authorized to delete")
        @WithUserDetails("admin2@expmatik.com")
        void testDeleteMaintenanceDetail_UserNotAuthorized_shouldReturn403() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.delete("/api/maintenances/{maintenanceId}/details/{detailId}", "00000000-0000-0000-0000-000000000003", "00000000-0000-0000-0000-000000000003")
                                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 403 when user is maintainer")
        @WithUserDetails("repo@expmatik.com")
        void testDeleteMaintenanceDetail_UserIsMaintainer_shouldReturn403() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.delete("/api/maintenances/{maintenanceId}/details/{detailId}", "00000000-0000-0000-0000-000000000003", "00000000-0000-0000-0000-000000000003")
                                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }


    }


}
