package com.example.sales_expense_system.controller;

import com.example.sales_expense_system.dto.request.SaleRequest;
import com.example.sales_expense_system.dto.request.SaleUpdateRequest;
import com.example.sales_expense_system.dto.response.PaginatedResponse;
import com.example.sales_expense_system.dto.response.SaleResponse;
import com.example.sales_expense_system.model.Sale;
import com.example.sales_expense_system.service.SaleService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<List<SaleResponse>> getAllSales() {
        List<SaleResponse> response = saleService.getAllSales()
                .stream()
                .map(this::mapToDto)
                .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<SaleResponse> createSale(@Valid @RequestBody SaleRequest request, HttpServletRequest httpRequest) {
        Sale sale = saleService.createSale(request, httpRequest);
        return ResponseEntity.ok(mapToDto(sale));
    }

    @GetMapping("/advanced")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<PaginatedResponse<SaleResponse>> getSalesAdvanced(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "false") boolean includeVoided,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionDate,desc") String sort
    ) {
        
        Sort sortObj = parseSort(sort);

        Page<Sale> result = saleService.getSalesAdvanced(
                keyword,
                startDate != null ? LocalDate.parse(startDate) : null,
                endDate != null ? LocalDate.parse(endDate) : null,
                categoryId,
                includeVoided,
                page,
                size,
                sortObj
        );

        List<SaleResponse> content = result.getContent()
                .stream()
                .map(this::mapToDto)
                .toList();

        PaginatedResponse<SaleResponse> response =
                new PaginatedResponse<>(
                        content,
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages()
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<List<SaleResponse>> searchSales(@RequestParam String keyword) {
        List<SaleResponse> response = saleService.searchSales(keyword)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<SaleResponse> getSaleById(@PathVariable Long id) {
        return ResponseEntity.ok(mapToDto(saleService.getSaleById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<SaleResponse> updateSale(@PathVariable Long id,
                                                   @Valid @RequestBody SaleUpdateRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(mapToDto(saleService.updateSale(id, request, httpRequest)));
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<SaleResponse> voidSale(@PathVariable Long id,
                                                 @RequestParam String reason, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(mapToDto(saleService.voidSale(id, reason, httpRequest)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<String> permanentDeleteSale(@PathVariable Long id,
                                                  HttpServletRequest httpRequest) {
        saleService.permanentDeleteSale(id, httpRequest);
        return ResponseEntity.ok("Sale permanently deleted successfully");
    }

    @GetMapping("/reports/daily-total")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<BigDecimal> getDailySalesTotal(@RequestParam String date) {
        return ResponseEntity.ok(saleService.getDailySalesTotal(LocalDate.parse(date)));
    }

    @GetMapping("/reports/monthly-total")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<BigDecimal> getMonthlySalesTotal(@RequestParam int month,
                                                           @RequestParam int year) {
        return ResponseEntity.ok(saleService.getMonthlySalesTotal(month, year));
    }

    private SaleResponse mapToDto(Sale sale) {
            SaleResponse response = new SaleResponse();
            response.setSaleId(sale.getSaleId());
            response.setOrNumber(sale.getSaleNumber());
            response.setItemName(sale.getItemName());
            response.setCustomerName(sale.getCustomerName());
            response.setCategoryId(sale.getCategory() != null ? sale.getCategory().getCategoryId() : null);
            response.setCategoryName(sale.getCategory() != null ? sale.getCategory().getCategoryName() : null);
            response.setQuantity(sale.getQuantity());
            response.setUnitPrice(sale.getUnitPrice());
            response.setTotalAmount(sale.getTotalAmount());
            response.setTransactionDate(sale.getTransactionDate());
            response.setVoided(Boolean.TRUE.equals(sale.getIsVoided()));
            response.setVoidReason(sale.getVoidReason());
            response.setVoidedAt(sale.getVoidedAt());
            response.setVoidedByUserId(sale.getVoidedBy() != null ? sale.getVoidedBy().getUserId() : null);
            response.setVoidedByUsername(sale.getVoidedBy() != null ? sale.getVoidedBy().getUsername() : null);
            response.setCreatedByUserId(sale.getCreatedBy() != null ? sale.getCreatedBy().getUserId() : null);
            response.setCreatedByUsername(sale.getCreatedBy() != null ? sale.getCreatedBy().getUsername() : null);
            return response;
        }

        private Sort parseSort(String sort) {
        try {
            String[] parts = sort.split(",");
            String field = parts[0];

            // whitelist fields (VERY IMPORTANT for security)
            List<String> allowed = List.of(
                    "transactionDate",
                    "saleId",
                    "saleNumber",
                    "unitPrice",
                    "totalAmount",
                    "quantity",
                    "itemName"
            );

            if (!allowed.contains(field)) {
                field = "transactionDate";
            }

            Sort.Direction direction =
                    parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                            ? Sort.Direction.ASC
                            : Sort.Direction.DESC;

            return Sort.by(direction, field);

        } catch (Exception e) {
            return Sort.by(Sort.Direction.DESC, "transactionDate");
        }
    }
}