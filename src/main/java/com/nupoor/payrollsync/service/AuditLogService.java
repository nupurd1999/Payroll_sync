package com.nupoor.payrollsync.service;

import com.nupoor.payrollsync.entity.AuditLog;
import com.nupoor.payrollsync.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final String secretKey;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           @Value("${payroll.audit.secret-key:SuperSecretHmacKeyForFinancialAuditLogs2026}") String secretKey) {
        this.auditLogRepository = auditLogRepository;
        this.secretKey = secretKey;
    }

    public AuditLog logAction(String entityName, String entityId, String action, String payloadJson) {
        String hmacSignature = calculateHmac(entityName + ":" + entityId + ":" + action + ":" + payloadJson);
        AuditLog auditLog = new AuditLog(entityName, entityId, action, payloadJson, hmacSignature);
        return auditLogRepository.save(auditLog);
    }

    public java.util.List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    public boolean verifyIntegrity(AuditLog auditLog) {
        if (auditLog == null) return false;
        String recalculated = calculateHmac(auditLog.getEntityName() + ":" + auditLog.getEntityId() + ":" + auditLog.getAction() + ":" + auditLog.getPayloadJson());
        return recalculated.equalsIgnoreCase(auditLog.getHmacSignature());
    }

    public String calculateHmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder sb = new StringBuilder(hmacBytes.length * 2);
            for (byte b : hmacBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256 signature", e);
        }
    }
}
