package com.nupoor.payrollsync.service;

import com.nupoor.payrollsync.entity.Employee;
import com.nupoor.payrollsync.entity.PayrollBatch;
import com.nupoor.payrollsync.entity.PayrollItem;
import com.nupoor.payrollsync.repository.EmployeeRepository;
import com.nupoor.payrollsync.repository.PayrollBatchRepository;
import com.nupoor.payrollsync.repository.PayrollItemRepository;
import com.nupoor.payrollsync.strategy.TaxCalculatorFactory;
import com.nupoor.payrollsync.strategy.TaxCalculatorStrategy;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PayrollBatchService {

    private static final Logger log = LoggerFactory.getLogger(PayrollBatchService.class);

    private final PayrollBatchRepository batchRepository;
    private final PayrollItemRepository itemRepository;
    private final EmployeeRepository employeeRepository;
    private final TaxCalculatorFactory taxCalculatorFactory;
    private final AuditLogService auditLogService;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Autowired(required = false)
    private RedissonClient redissonClient;

    public PayrollBatchService(PayrollBatchRepository batchRepository,
                               PayrollItemRepository itemRepository,
                               EmployeeRepository employeeRepository,
                               TaxCalculatorFactory taxCalculatorFactory,
                               AuditLogService auditLogService,
                               @Autowired(required = false) SimpMessagingTemplate messagingTemplate) {
        this.batchRepository = batchRepository;
        this.itemRepository = itemRepository;
        this.employeeRepository = employeeRepository;
        this.taxCalculatorFactory = taxCalculatorFactory;
        this.auditLogService = auditLogService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public PayrollBatch createPayrollBatch(String period, String idempotencyKey) {
        batchRepository.findByIdempotencyKey(idempotencyKey).ifPresent(b -> {
            throw new IllegalStateException("Duplicate batch request for idempotency key: " + idempotencyKey);
        });

        String ref = "BATCH-" + period + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        PayrollBatch batch = new PayrollBatch(null, ref, period, "CREATED", idempotencyKey);
        batch = batchRepository.save(batch);

        List<Employee> employees = employeeRepository.findAll();
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (Employee emp : employees) {
            TaxCalculatorStrategy strategy = taxCalculatorFactory.getStrategy(emp.getCountryCode());
            TaxCalculatorStrategy.TaxBreakdown breakdown = strategy.calculateTax(emp.getBaseSalary(), emp.getTaxClass());

            PayrollItem item = new PayrollItem(
                    batch, emp, emp.getBaseSalary(),
                    breakdown.incomeTax(), breakdown.healthInsurance().add(breakdown.solidarityTax()),
                    breakdown.netSalary(), "PENDING"
            );
            itemRepository.save(item);

            totalGross = totalGross.add(emp.getBaseSalary());
            totalTax = totalTax.add(breakdown.totalDeduction());
            totalNet = totalNet.add(breakdown.netSalary());
        }

        batch.setTotalGross(totalGross);
        batch.setTotalTax(totalTax);
        batch.setTotalNet(totalNet);
        batch.setStatus("CALCULATED");
        batch = batchRepository.save(batch);

        auditLogService.logAction("PayrollBatch", batch.getId().toString(), "BATCH_CREATED", 
                "{\"reference\":\"" + ref + "\",\"period\":\"" + period + "\",\"items\":" + employees.size() + "}");

        return batch;
    }

    public void processPayrollBatch(UUID batchId) {
        if (redissonClient != null) {
            String lockKey = "lock:payroll_batch:" + batchId;
            RLock lock = redissonClient.getLock(lockKey);
            try {
                boolean acquired = lock.tryLock(5, 60, TimeUnit.SECONDS);
                if (!acquired) {
                    log.warn("Concurrent execution blocked for Batch ID: {}", batchId);
                    throw new IllegalStateException("Batch is currently being processed by another worker.");
                }
                executeBatch(batchId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrupted acquiring lock for batch", e);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } else {
            executeBatch(batchId);
        }
    }

    @Transactional
    public void executeBatch(UUID batchId) {
        PayrollBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found for ID: " + batchId));

        if ("COMPLETED".equals(batch.getStatus())) {
            log.info("Batch {} already completed. Idempotent skip.", batchId);
            return;
        }

        batch.setStatus("PROCESSING");
        batchRepository.save(batch);

        List<PayrollItem> items = itemRepository.findByBatchId(batchId);
        int total = items.size();

        for (int i = 0; i < total; i++) {
            PayrollItem item = items.get(i);
            if ("SUCCESS".equals(item.getStatus())) continue;

            item.setStatus("SUCCESS");
            item.setTransactionReference("SEPA-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            item.setProcessedAt(ZonedDateTime.now());
            itemRepository.save(item);

            int progressPercent = total > 0 ? (int) (((double) (i + 1) / total) * 100) : 100;
            if (messagingTemplate != null) {
                try {
                    messagingTemplate.convertAndSend("/topic/payroll-progress/" + batchId,
                            new ProgressUpdate(batchId.toString(), i + 1, total, progressPercent));
                } catch (Exception e) {
                    log.warn("WebSocket broadcast failed: {}", e.getMessage());
                }
            }
        }

        batch.setStatus("COMPLETED");
        batchRepository.save(batch);

        auditLogService.logAction("PayrollBatch", batchId.toString(), "BATCH_DISBURSED",
                "{\"batchId\":\"" + batchId + "\",\"processedItems\":" + total + "}");
    }

    public static class ProgressUpdate {
        private final String batchId;
        private final int processed;
        private final int total;
        private final int percent;

        public ProgressUpdate(String batchId, int processed, int total, int percent) {
            this.batchId = batchId;
            this.processed = processed;
            this.total = total;
            this.percent = percent;
        }

        public String getBatchId() { return batchId; }
        public int getProcessed() { return processed; }
        public int getTotal() { return total; }
        public int getPercent() { return percent; }
    }
}
