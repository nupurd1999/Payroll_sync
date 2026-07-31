package com.nupoor.payrollsync.controller;

import com.nupoor.payrollsync.entity.AuditLog;
import com.nupoor.payrollsync.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAuditLogs() {
        List<AuditLog> logs = auditLogService.getAllAuditLogs();
        
        List<Map<String, Object>> response = logs.stream().map(log -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", log.getId());
            item.put("entityName", log.getEntityName());
            item.put("entityId", log.getEntityId());
            item.put("action", log.getAction());
            item.put("payloadJson", log.getPayloadJson());
            item.put("hmacSignature", log.getHmacSignature());
            item.put("createdAt", log.getCreatedAt());
            item.put("verified", auditLogService.verifyIntegrity(log));
            return item;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
