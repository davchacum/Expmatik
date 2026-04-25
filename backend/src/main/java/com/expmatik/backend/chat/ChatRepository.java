package com.expmatik.backend.chat;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRepository extends JpaRepository<Chat, UUID> {

    @Query("""
        SELECT c FROM Chat c
        WHERE c.maintenance.id = :maintenanceId
        ORDER BY c.sentAt ASC
    """)
    List<Chat> findByMaintenanceId(@Param("maintenanceId") UUID maintenanceId);

}
