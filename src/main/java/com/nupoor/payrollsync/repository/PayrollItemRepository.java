package com.nupoor.payrollsync.repository;

import com.nupoor.payrollsync.entity.PayrollItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayrollItemRepository extends JpaRepository<PayrollItem, UUID> {
    List<PayrollItem> findByBatchId(UUID batchId);
    Optional<PayrollItem> findByBatchIdAndEmployeeId(UUID batchId, UUID employeeId);
}
