package com.nupoor.payrollsync.service;

import com.nupoor.payrollsync.entity.PayrollBatch;
import com.nupoor.payrollsync.repository.EmployeeRepository;
import com.nupoor.payrollsync.repository.PayrollBatchRepository;
import com.nupoor.payrollsync.repository.PayrollItemRepository;
import com.nupoor.payrollsync.strategy.TaxCalculatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PayrollBatchServiceConcurrencyTest {

    private PayrollBatchRepository batchRepository;
    private PayrollItemRepository itemRepository;
    private EmployeeRepository employeeRepository;
    private TaxCalculatorFactory taxCalculatorFactory;
    private AuditLogService auditLogService;
    private RedissonClient redissonClient;
    private RLock lock;

    private PayrollBatchService payrollBatchService;
    private UUID testBatchId;

    @BeforeEach
    void setUp() throws Exception {
        batchRepository = mock(PayrollBatchRepository.class);
        itemRepository = mock(PayrollItemRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        taxCalculatorFactory = mock(TaxCalculatorFactory.class);
        auditLogService = mock(AuditLogService.class);
        redissonClient = mock(RedissonClient.class);
        lock = mock(RLock.class);

        testBatchId = UUID.randomUUID();
        PayrollBatch batch = new PayrollBatch(testBatchId, "REF-001", "2026-07", "CALCULATED", "IDEM-001");

        when(batchRepository.findById(testBatchId)).thenReturn(Optional.of(batch));
        when(redissonClient.getLock(anyString())).thenReturn(lock);

        payrollBatchService = new PayrollBatchService(
                batchRepository, itemRepository, employeeRepository,
                taxCalculatorFactory, auditLogService, null
        );

        // Inject redisson client via reflection or field injection
        try {
            var field = PayrollBatchService.class.getDeclaredField("redissonClient");
            field.setAccessible(true);
            field.set(payrollBatchService, redissonClient);
        } catch (Exception e) {
            fail("Failed to set redissonClient on service");
        }
    }

    @Test
    @DisplayName("Should guarantee exactly 1 processing execution when 10 threads hit processPayrollBatch concurrently")
    void testConcurrentBatchExecution_ShouldExecuteOnlyOnce() throws Exception {
        int numberOfThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // First thread gets lock, remaining 9 fail to acquire lock
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true)
                .thenReturn(false);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    latch.await(); // Synchronize all threads start
                    payrollBatchService.processPayrollBatch(testBatchId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            });
        }

        latch.countDown(); // Trigger threads simultaneously
        executor.shutdown();
        boolean terminated = executor.awaitTermination(5, TimeUnit.SECONDS);

        assertTrue(terminated, "Executor service should terminate in time");
        assertEquals(1, successCount.get(), "Exactly 1 thread should successfully execute batch processing");
        assertEquals(9, failureCount.get(), "9 concurrent threads should be rejected due to distributed lock");
    }
}
