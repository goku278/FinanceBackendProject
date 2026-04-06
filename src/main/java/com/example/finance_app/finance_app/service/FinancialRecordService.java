package com.example.finance_app.finance_app.service;

import com.example.finance_app.finance_app.entity.FinancialRecord;
import com.example.finance_app.finance_app.exceptions.ResourceNotFoundException;
import com.example.finance_app.finance_app.models.TransactionType;
import com.example.finance_app.finance_app.models.dto.DashboardDTO;
import com.example.finance_app.finance_app.models.dto.FinancialRecordDTO;
import com.example.finance_app.finance_app.repository.FinancialRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinancialRecordService {
    private final FinancialRecordRepository recordRepository;

    @Transactional
    public FinancialRecord createRecord(FinancialRecordDTO.RecordRequest request) {
        FinancialRecord record = FinancialRecord.builder()
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory())
                .date(request.getDate())
                .description(request.getDescription())
                .build();
        return recordRepository.save(record);
    }

    @Transactional
    public FinancialRecord updateRecord(Long id, FinancialRecordDTO.RecordRequest request) {
        FinancialRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found with id: " + id));
        record.setAmount(request.getAmount());
        record.setType(request.getType());
        record.setCategory(request.getCategory());
        record.setDate(request.getDate());
        record.setDescription(request.getDescription());
        return recordRepository.save(record);
    }

    @Transactional
    public void deleteRecord(Long id) {
        FinancialRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found with id: " + id));
        record.setDeleted(true);
        recordRepository.save(record);
    }

    public Page<FinancialRecord> getAllRecords(LocalDate startDate, LocalDate endDate,
                                               String category, TransactionType type,
                                               String description,
                                               Pageable pageable) {
        return recordRepository.findAllWithFilters(startDate, endDate, category, type, description, pageable);
    }

    public DashboardDTO.Summary getSummary() {
        BigDecimal totalIncome = recordRepository.sumByType(TransactionType.INCOME);
        BigDecimal totalExpense = recordRepository.sumByType(TransactionType.EXPENSE);
        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;
        return DashboardDTO.Summary.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netBalance(totalIncome.subtract(totalExpense))
                .build();
    }

    public List<DashboardDTO.CategoryTotal> getCategoryTotals(TransactionType type) {
        List<Object[]> results = recordRepository.sumGroupByCategory(type);
        return results.stream()
                .map(row -> DashboardDTO.CategoryTotal.builder()
                        .category((String) row[0])
                        .total((BigDecimal) row[1])
                        .build())
                .collect(Collectors.toList());
    }

    public List<FinancialRecordDTO.RecordResponse> getRecentActivity() {
        return recordRepository.findTop5ByDeletedFalseOrderByDateDescCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<DashboardDTO.MonthlyTrend> getMonthlyTrends() {
        List<Object[]> results = recordRepository.findMonthlyTrends();
        List<DashboardDTO.MonthlyTrend> trends = new ArrayList<>();
        for (Object[] row : results) {
            Integer year = ((Number) row[0]).intValue();
            Integer month = ((Number) row[1]).intValue();
            BigDecimal income = (BigDecimal) row[2];
            BigDecimal expense = (BigDecimal) row[3];
            trends.add(DashboardDTO.MonthlyTrend.builder()
                    .year(year)
                    .month(month)
                    .income(income != null ? income : BigDecimal.ZERO)
                    .expense(expense != null ? expense : BigDecimal.ZERO)
                    .build());
        }
        return trends;
    }

    private FinancialRecordDTO.RecordResponse toResponse(FinancialRecord record) {
        return FinancialRecordDTO.RecordResponse.builder()
                .id(record.getId())
                .amount(record.getAmount())
                .type(record.getType())
                .category(record.getCategory())
                .date(record.getDate())
                .description(record.getDescription())
                .build();
    }
}