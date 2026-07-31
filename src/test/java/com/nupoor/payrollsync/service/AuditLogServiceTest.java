package com.nupoor.payrollsync.service;

import com.nupoor.payrollsync.entity.AuditLog;
import com.nupoor.payrollsync.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditLogServiceTest {

    private AuditLogRepository auditLogRepository;
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        auditLogService = new AuditLogService(auditLogRepository, "SecretTestKey123");

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Should generate valid HMAC-SHA256 signature and verify audit integrity")
    void testHmacSigningAndVerification() {
        AuditLog log = auditLogService.logAction("PayrollBatch", "BATCH-001", "BATCH_CREATED", "{\"amount\":5000}");

        assertNotNull(log);
        assertNotNull(log.getHmacSignature());
        assertEquals(64, log.getHmacSignature().length()); // SHA-256 hex string length

        assertTrue(auditLogService.verifyIntegrity(log));
    }

    @Test
    @DisplayName("Should detect tampered payload during audit verification")
    void testTamperDetection() {
        AuditLog log = auditLogService.logAction("PayrollBatch", "BATCH-001", "BATCH_CREATED", "{\"amount\":5000}");

        // Simulate hacker modifying the payload JSON
        log.setPayloadJson("{\"amount\":999999}");

        assertFalse(auditLogService.verifyIntegrity(log));
    }
}
