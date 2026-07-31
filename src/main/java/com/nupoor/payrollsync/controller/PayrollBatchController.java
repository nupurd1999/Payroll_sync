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

    @PostMapping("/{id}/process")
    public ResponseEntity<Map<String, String>> processBatch(@PathVariable UUID id) {
        payrollBatchService.processPayrollBatch(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Batch processing triggered successfully.");
        response.put("batchId", id.toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getBatchDetails(@PathVariable UUID id) {
        PayrollBatch batch = batchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found for ID: " + id));
        List<PayrollItem> items = itemRepository.findByBatchId(id);

        Map<String, Object> result = new HashMap<>();
        result.put("batch", batch);
        result.put("items", items);
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
