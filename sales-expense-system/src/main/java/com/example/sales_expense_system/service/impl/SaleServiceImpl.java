package com.example.sales_expense_system.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;

import jakarta.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.sales_expense_system.dto.AuditFieldChangeDto;
import com.example.sales_expense_system.dto.request.SaleRequest;
import com.example.sales_expense_system.dto.request.SaleUpdateRequest;
import com.example.sales_expense_system.exception.ApiException;
import com.example.sales_expense_system.model.AccountingPeriod;
import com.example.sales_expense_system.model.Category;
import com.example.sales_expense_system.model.Sale;
import com.example.sales_expense_system.model.User;
import com.example.sales_expense_system.repository.AccountingPeriodRepository;
import com.example.sales_expense_system.repository.CategoryRepository;
import com.example.sales_expense_system.repository.SaleRepository;
import com.example.sales_expense_system.repository.UserRepository;
import com.example.sales_expense_system.service.AuditLogService;
import com.example.sales_expense_system.service.SaleService;
import com.example.sales_expense_system.util.AuditActions;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
public class SaleServiceImpl implements SaleService {

    private final SaleRepository saleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AccountingPeriodRepository periodRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public SaleServiceImpl(SaleRepository saleRepository,
                           UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           AccountingPeriodRepository periodRepository,
                           AuditLogService auditLogService,
                           ObjectMapper objectMapper) {
        this.saleRepository = saleRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.periodRepository = periodRepository;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper; 
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sale> getAllSales() {
        List<Sale> sales = saleRepository.findByIsVoidedFalseOrderByTransactionDateDescSaleIdDesc();
        sales.forEach(this::initializeSaleRelations);
        return sales;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Sale> getSalesAdvanced(
            String keyword,
            LocalDate startDate,
            LocalDate endDate,
            Long categoryId,
            boolean includeVoided,
            int page,
            int size,
            Sort sort) {

        if (size > 100) size = 100; // prevent abuse

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Sale> result = saleRepository.searchWithFilters(
            keyword == null || keyword.isBlank() ? null : keyword.trim(),
            categoryId,
            startDate,
            endDate,
            includeVoided,
            pageable
        );

        result.getContent().forEach(this::initializeSaleRelations);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sale> getAllSalesIncludingVoided() {
        List<Sale> sales = saleRepository.findAllByOrderByTransactionDateDescSaleIdDesc();
        sales.forEach(this::initializeSaleRelations);
        return sales;
    }

    @Override
    @Transactional(readOnly = true)
    public Sale getSaleById(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ApiException("Sale not found", HttpStatus.NOT_FOUND));
        initializeSaleRelations(sale);
        return sale;
    }

    @Override
    @Transactional
    public Sale createSale(SaleRequest request, HttpServletRequest httpRequest) {

        Category category = categoryRepository.findById(request.getCategoryId().longValue())
                .orElseThrow(() -> new ApiException("Category not found", HttpStatus.NOT_FOUND));

        if (category.getType() != Category.CategoryType.SALES) {
            throw new ApiException("Selected category is not a SALES category", HttpStatus.BAD_REQUEST);
        }

        AccountingPeriod period = findPeriodByTransactionDate(request.getTransactionDate());
        validatePeriodNotLocked(period);

        User currentUser = getCurrentUser();

        String orNumber = null;
        if (request.getOrNumber() != null && !request.getOrNumber().trim().isEmpty()) {
            orNumber = request.getOrNumber().trim();
        }

        Sale sale = new Sale();
        sale.setSaleNumber(orNumber);
        sale.setTransactionDate(request.getTransactionDate());
        sale.setItemName(request.getItemName().trim());
        sale.setCustomerName(request.getCustomerName() != null ? request.getCustomerName().trim() : null);
        sale.setCategory(category);
        sale.setQuantity(request.getQuantity());
        sale.setUnitPrice(request.getUnitPrice());
        sale.setIsVoided(false);
        sale.setCreatedBy(currentUser);
        sale.setPeriod(period);

        Sale savedSale;

        try {
            savedSale = saleRepository.save(sale);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ApiException("OR number already exists", HttpStatus.CONFLICT);
        }

        List<AuditFieldChangeDto> changes = buildCreateChanges(savedSale);

        auditLogService.log(
                AuditActions.CREATE_SALE,
                "Sales",
                "sales",
                savedSale.getSaleId(),
                saleTargetDisplay(savedSale),
                "Created sale " + saleTargetDisplay(savedSale) + ".",
                "New sale record was added.",
                null,
                saleSnapshot(savedSale),
                toJson(changes),
                currentUser,
                httpRequest
        );

        initializeSaleRelations(savedSale);

        return savedSale;
    }

    @Override
    @Transactional
    public Sale updateSale(Long id, SaleUpdateRequest request, HttpServletRequest httpRequest) {

        Sale existing = getSaleById(id);

        // 🚨 STEP 2A — BLOCK if already VOIDED
        if (Boolean.TRUE.equals(existing.getIsVoided())) {
            throw new ApiException("Voided sales cannot be edited", HttpStatus.CONFLICT);
        }

        // 🚨 STEP 2B — OPTIMISTIC LOCK CHECK
        if (request.getVersion() == null || !existing.getVersion().equals(request.getVersion())) {
            throw new ApiException("Data has been modified by another user", HttpStatus.CONFLICT);
        }

        validatePeriodNotLocked(existing.getPeriod());

        User currentUser = getCurrentUser();
        validateCanModifySale(existing, currentUser);

        String oldSaleNumber = existing.getSaleNumber();
        String oldTransactionDate = String.valueOf(existing.getTransactionDate());
        String oldItemName = existing.getItemName();
        String oldCustomerName = existing.getCustomerName();
        String oldCategoryName = existing.getCategory() != null ? existing.getCategory().getCategoryName() : "";
        String oldQuantity = stringifyNumber(existing.getQuantity());
        String oldUnitPrice = stringifyNumber(existing.getUnitPrice());
        String oldPeriodName = existing.getPeriod() != null ? existing.getPeriod().getName() : "";
        String oldValues = saleSnapshot(existing);

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId().longValue())
                    .orElseThrow(() -> new ApiException("Category not found", HttpStatus.NOT_FOUND));

            if (category.getType() != Category.CategoryType.SALES) {
                throw new ApiException("Selected category is not a SALES category", HttpStatus.BAD_REQUEST);
            }

            existing.setCategory(category);
        }

        if (request.getItemName() != null) {
            String itemName = request.getItemName().trim();
            if (itemName.isEmpty()) {
                throw new ApiException("Item name must not be blank", HttpStatus.BAD_REQUEST);
            }
            existing.setItemName(itemName);
        }

        if (request.getCustomerName() != null) {
            String customerName = request.getCustomerName().trim();
            existing.setCustomerName(customerName.isEmpty() ? null : customerName);
        }

        if (request.getQuantity() != null) {
            if (request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiException("Quantity must be greater than 0", HttpStatus.BAD_REQUEST);
            }
            existing.setQuantity(request.getQuantity());
        }

        if (request.getUnitPrice() != null) {
            if (request.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiException("Unit price must be greater than 0", HttpStatus.BAD_REQUEST);
            }
            existing.setUnitPrice(request.getUnitPrice());
        }

        if (request.getOrNumber() != null) {
            String trimmedOrNumber = request.getOrNumber().trim();
                
            if (!trimmedOrNumber.isEmpty()
                && !trimmedOrNumber.equals(existing.getSaleNumber())) {
                
                boolean exists = saleRepository.existsBySaleNumberAndSaleIdNot(
                    trimmedOrNumber, existing.getSaleId()
                );
                
                if (exists) {
                    throw new ApiException("OR number already exists", HttpStatus.CONFLICT);
                }
            }
        
            existing.setSaleNumber(trimmedOrNumber.isEmpty() ? null : trimmedOrNumber);
        }

        if (request.getTransactionDate() != null) {
            AccountingPeriod newPeriod = findPeriodByTransactionDate(request.getTransactionDate());
            validatePeriodNotLocked(newPeriod);
            existing.setTransactionDate(request.getTransactionDate());
            existing.setPeriod(newPeriod);
        }

        List<AuditFieldChangeDto> changes = new ArrayList<>();
        addChangedField(changes, "saleNumber", oldSaleNumber, existing.getSaleNumber());
        addChangedField(changes, "transactionDate", oldTransactionDate, String.valueOf(existing.getTransactionDate()));
        addChangedField(changes, "itemName", oldItemName, existing.getItemName());
        addChangedField(changes, "customerName", oldCustomerName, existing.getCustomerName());
        addChangedField(changes, "category", oldCategoryName, existing.getCategory() != null ? existing.getCategory().getCategoryName() : "");
        addChangedField(changes, "quantity", oldQuantity, stringifyNumber(existing.getQuantity()));
        addChangedField(changes, "unitPrice", oldUnitPrice, stringifyNumber(existing.getUnitPrice()));
        addChangedField(changes, "period", oldPeriodName, existing.getPeriod() != null ? existing.getPeriod().getName() : "");

        Sale updatedSale;

        try {
            updatedSale = saleRepository.save(existing);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ApiException("OR number already exists", HttpStatus.CONFLICT);
        }

        auditLogService.log(
                AuditActions.UPDATE_SALE,
                "Sales",
                "sales",
                updatedSale.getSaleId(),
                saleTargetDisplay(updatedSale),
                "Updated sale " + saleTargetDisplay(updatedSale) + ".",
                changes.isEmpty()
                        ? "Sale record was saved with no detected field changes."
                        : "Sale details were updated.",
                oldValues,
                saleSnapshot(updatedSale),
                toJson(changes),
                currentUser,
                httpRequest
        );

        initializeSaleRelations(updatedSale);
        return updatedSale;
    }

    @Override
    @Transactional
    public Sale voidSale(Long id, String reason, Long version, HttpServletRequest httpRequest) {

        Sale sale = getSaleById(id);

        // Check for voided first — gives a clearer error than "stale data" when
        // the record was already voided by someone else between load and submit.
        if (Boolean.TRUE.equals(sale.getIsVoided())) {
            throw new ApiException("Sale is already voided", HttpStatus.CONFLICT);
        }

        // Optimistic lock check: reject if the client's version doesn't match the DB.
        // This catches the case where another user edited the sale after this user
        // loaded it — sale.getVersion() is never null for a persisted entity, so
        // the old "== null" guard was a no-op that protected nothing.
        if (version == null || !sale.getVersion().equals(version)) {
            throw new ApiException("Data has been modified by another user", HttpStatus.CONFLICT);
        }

        validatePeriodNotLocked(sale.getPeriod());

        User currentUser = getCurrentUser();
        validateCanModifySale(sale, currentUser);

        String oldValues = saleSnapshot(sale);

        sale.setIsVoided(true);
        sale.setVoidedBy(currentUser);
        sale.setVoidedAt(LocalDateTime.now());
        sale.setVoidReason(reason == null || reason.trim().isEmpty() ? null : reason.trim());

        Sale voidedSale = saleRepository.save(sale);

        List<AuditFieldChangeDto> changes = List.of(
            new AuditFieldChangeDto("isVoided", "false", "true"),
            new AuditFieldChangeDto("voidReason", "", safe(voidedSale.getVoidReason())),
            new AuditFieldChangeDto("voidedBy", "", currentUser.getUsername()),
            new AuditFieldChangeDto("voidedAt", "", String.valueOf(voidedSale.getVoidedAt()))
        );

        auditLogService.log(
                AuditActions.VOID_SALE,
                "Sales",
                "sales",
                voidedSale.getSaleId(),
                saleTargetDisplay(voidedSale),
                "Voided sale " + saleTargetDisplay(voidedSale) + ".",
                voidedSale.getVoidReason() == null
                        ? "Sale was marked as void."
                        : "Sale was marked as void. Reason: " + voidedSale.getVoidReason(),
                oldValues,
                saleSnapshot(voidedSale),
                toJson(changes),
                currentUser,
                httpRequest
        );

        initializeSaleRelations(voidedSale);
        return voidedSale;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sale> searchSales(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return saleRepository.findByIsVoidedFalseOrderByTransactionDateDescSaleIdDesc();
        }

        String search = keyword.trim();

        List<Sale> byItemName =
                saleRepository.findByItemNameContainingIgnoreCaseAndIsVoidedFalseOrderByTransactionDateDescSaleIdDesc(search);

        List<Sale> bySaleNumber =
                saleRepository.findBySaleNumberContainingIgnoreCaseAndIsVoidedFalseOrderByTransactionDateDescSaleIdDesc(search);

        List<Sale> results = new ArrayList<>(byItemName);

        for (Sale sale : bySaleNumber) {
            if (!results.contains(sale)) {
                results.add(sale);
            }
        }

        results.forEach(this::initializeSaleRelations);
        return results;
    }

    @Override
    public BigDecimal getDailySalesTotal(LocalDate date) {
        return saleRepository.sumSalesByDate(date);
    }

    @Override
    public BigDecimal getMonthlySalesTotal(int month, int year) {
        return saleRepository.sumSalesByMonthAndYear(month, year);
    }

    private AccountingPeriod findPeriodByTransactionDate(LocalDate transactionDate) {
        return periodRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(transactionDate, transactionDate)
                .orElseThrow(() -> new ApiException(
                "No accounting period found for transaction date",
                HttpStatus.NOT_FOUND
            ));
    }

    private void validatePeriodNotLocked(AccountingPeriod period) {
        if (Boolean.TRUE.equals(period.getIsLocked())) {
            throw new ApiException(
                    "Accounting period is locked: " + period.getName()
                            + " (" + period.getStartDate() + " to " + period.getEndDate() + ")", HttpStatus.CONFLICT
            );
        }
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(
                "Authenticated user not found",
                HttpStatus.UNAUTHORIZED
            ));
    }

    private void validateCanModifySale(Sale sale, User currentUser) {
        boolean isAdmin = currentUser.getRole() != null
                && currentUser.getRole().getRoleName() != null
                && currentUser.getRole().getRoleName().equalsIgnoreCase("ADMIN");

        boolean isOwner = sale.getCreatedBy() != null
                && sale.getCreatedBy().getUserId().equals(currentUser.getUserId());

        if (!isAdmin && !isOwner) {
            throw new ApiException("You do not have permission to modify this sale", HttpStatus.FORBIDDEN);
        }
    }

    @Transactional
    @Override
    public void permanentDeleteSale(Long id, HttpServletRequest httpRequest) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ApiException("Sale not found", HttpStatus.NOT_FOUND));

        if (!Boolean.TRUE.equals(sale.getIsVoided())) {
            throw new ApiException(
            "Only voided sales can be permanently deleted",
            HttpStatus.BAD_REQUEST
        );
        }

        User admin = getCurrentUser();
        String oldValues = saleSnapshot(sale);
        String targetDisplay = saleTargetDisplay(sale);

        List<AuditFieldChangeDto> changes = List.of(
            new AuditFieldChangeDto("deleted", "false", "true")
        );

        auditLogService.log(
                AuditActions.PERMANENT_DELETE_SALE,
                "Sales",
                "sales",
                sale.getSaleId(),
                targetDisplay,
                "Permanently deleted sale " + targetDisplay + ".",
                "Voided sale record was permanently deleted.",
                oldValues,
                null,
                toJson(changes),
                admin,
                httpRequest
        );

        saleRepository.delete(sale);
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String stringifyNumber(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private String saleTargetDisplay(Sale sale) {
        if (sale.getSaleNumber() != null && !sale.getSaleNumber().isBlank()) {
            return sale.getSaleNumber();
        }
        return "Sale #" + sale.getSaleId();
    }

    private String saleSnapshot(Sale sale) {
        return """
            {
              "saleNumber":"%s",
              "transactionDate":"%s",
              "itemName":"%s",
              "customerName":"%s",
              "category":"%s",
              "quantity":"%s",
              "unitPrice":"%s",
              "isVoided":%s,
              "voidReason":"%s",
              "period":"%s"
            }
            """.formatted(
                escapeJson(sale.getSaleNumber()),
                String.valueOf(sale.getTransactionDate()),
                escapeJson(sale.getItemName()),
                escapeJson(sale.getCustomerName()),
                escapeJson(sale.getCategory() != null ? sale.getCategory().getCategoryName() : ""),
                stringifyNumber(sale.getQuantity()),
                stringifyNumber(sale.getUnitPrice()),
                Boolean.TRUE.equals(sale.getIsVoided()),
                escapeJson(sale.getVoidReason()),
                escapeJson(sale.getPeriod() != null ? sale.getPeriod().getName() : "")
            ).replace("\n", "").replace("  ", "");
    }

    private void addChangedField(List<AuditFieldChangeDto> changes,
                                 String field,
                                 Object oldValue,
                                 Object newValue) {

        String oldText = oldValue == null ? "" : String.valueOf(oldValue);
        String newText = newValue == null ? "" : String.valueOf(newValue);

        if (!oldText.equals(newText)) {
            changes.add(new AuditFieldChangeDto(field, oldText, newText));
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private void initializeSaleRelations(Sale sale) {
        if (sale != null && sale.getCreatedBy() != null) {
            sale.getCreatedBy().getUserId();
            sale.getCreatedBy().getUsername();
        }
    
        if (sale != null && sale.getVoidedBy() != null) {
            sale.getVoidedBy().getUserId();
            sale.getVoidedBy().getUsername();
        }
    
        if (sale != null && sale.getPeriod() != null) {
            sale.getPeriod().getPeriodId();
            sale.getPeriod().getName();
            sale.getPeriod().getIsLocked();
        }
    
        if (sale != null && sale.getCategory() != null) {
            sale.getCategory().getCategoryId();
            sale.getCategory().getCategoryName();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private List<AuditFieldChangeDto> buildCreateChanges(Sale sale) {
        return List.of(
            new AuditFieldChangeDto("saleNumber", "", safe(sale.getSaleNumber())),
            new AuditFieldChangeDto("transactionDate", "", String.valueOf(sale.getTransactionDate())),
            new AuditFieldChangeDto("itemName", "", safe(sale.getItemName())),
            new AuditFieldChangeDto("customerName", "", safe(sale.getCustomerName())),
            new AuditFieldChangeDto("category", "", 
                sale.getCategory() != null ? safe(sale.getCategory().getCategoryName()) : ""),
            new AuditFieldChangeDto("quantity", "", stringifyNumber(sale.getQuantity())),
            new AuditFieldChangeDto("unitPrice", "", stringifyNumber(sale.getUnitPrice())),
            new AuditFieldChangeDto("isVoided", "", String.valueOf(sale.getIsVoided()))
        );
    }
}