package com.nupoor.payrollsync.controller;

import com.nupoor.payrollsync.dto.CreateBatchRequest;
import com.nupoor.payrollsync.entity.PayrollBatch;
import com.nupoor.payrollsync.entity.PayrollItem;
import com.nupoor.payrollsync.repository.PayrollBatchRepository;
import com.nupoor.payrollsync.repository.PayrollItemRepository;
import com.nupoor.payrollsync.service.PayrollBatchService;
import com.nupoor.payrollsync.service.SepaXmlGeneratorService;
import javax.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payroll/batches")
public class PayrollBatchController {

    private final PayrollBatchService payrollBatchService;
    private final PayrollBatchRepository batchRepository;
    private final PayrollItemRepository itemRepository;
    private final SepaXmlGeneratorService sepaXmlGeneratorService;

    public PayrollBatchController(PayrollBatchService payrollBatchService,
                                  PayrollBatchRepository batchRepository,
                                  PayrollItemRepository itemRepository,
                                  SepaXmlGeneratorService sepaXmlGeneratorService) {
        this.payrollBatchService = payrollBatchService;
        this.batchRepository = batchRepository;
        this.itemRepository = itemRepository;
        this.sepaXmlGeneratorService = sepaXmlGeneratorService;
    }

    @PostMapping
    public ResponseEntity<PayrollBatch> createBatch(@Valid @RequestBody CreateBatchRequest request) {
        PayrollBatch batch = payrollBatchService.createPayrollBatch(request.period(), request.idempotencyKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(batch);
    }

    @GetMapping
    public ResponseEntity<List<PayrollBatch>> getAllBatches() {
        return ResponseEntity.ok(batchRepository.findAll());
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<Map<String, String>> processBatch(@PathVariable UUID id) {
        payrollBatchService.processPayrollBatch(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Batch processing triggered successfully.");
        response.put("batchId", id.toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getBatchDetails(@PathVariable UUID id) {
        PayrollBatch batch = batchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found for ID: " + id));
        List<PayrollItem> items = itemRepository.findByBatchId(id);

        List<Map<String, Object>> itemDtos = items.stream().map(item -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", item.getId());
            dto.put("grossSalary", item.getGrossSalary());
            dto.put("taxDeductions", item.getTaxDeductions());
            dto.put("socialSecurityDeductions", item.getSocialSecurityDeductions());
            dto.put("netSalary", item.getNetSalary());
            dto.put("status", item.getStatus());
            dto.put("transactionReference", item.getTransactionReference());
            
            if (item.getEmployee() != null) {
                Map<String, Object> empMap = new HashMap<>();
                empMap.put("id", item.getEmployee().getId());
                empMap.put("employeeCode", item.getEmployee().getEmployeeCode());
                empMap.put("firstName", item.getEmployee().getFirstName());
                empMap.put("lastName", item.getEmployee().getLastName());
                empMap.put("email", item.getEmployee().getEmail());
                empMap.put("iban", item.getEmployee().getIban());
                empMap.put("bic", item.getEmployee().getBic());
                dto.put("employee", empMap);
            }
            return dto;
        }).collect(java.util.stream.Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("batch", batch);
        result.put("items", itemDtos);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/sepa-xml")
    public ResponseEntity<String> downloadSepaXml(@PathVariable UUID id) {
        PayrollBatch batch = batchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found for ID: " + id));
        List<PayrollItem> items = itemRepository.findByBatchId(id);

        String xmlContent = sepaXmlGeneratorService.generatePain001Xml(batch, items);
        String filename = "SEPA_PAIN001_" + batch.getBatchReference() + ".xml";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_XML)
                .body(xmlContent);
    }
}
