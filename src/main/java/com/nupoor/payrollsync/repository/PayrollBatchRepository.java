package com.nupoor.payrollsync.repository;

import com.nupoor.payrollsync.entity.PayrollBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayrollBatchRepository extends JpaRepository<PayrollBatch, UUID> {
    Optional<PayrollBatch> findByIdempotencyKey(String idempotencyKey);
    Optional<PayrollBatch> findByBatchReference(String batchReference);
}
