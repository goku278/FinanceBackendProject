package com.example.finance_app.finance_app;

import com.example.finance_app.finance_app.entity.FinancialRecord;
import com.example.finance_app.finance_app.exceptions.ResourceNotFoundException;
import com.example.finance_app.finance_app.models.TransactionType;
import com.example.finance_app.finance_app.models.dto.DashboardDTO;
import com.example.finance_app.finance_app.models.dto.FinancialRecordDTO;
import com.example.finance_app.finance_app.repository.FinancialRecordRepository;
import com.example.finance_app.finance_app.service.FinancialRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialRecordServiceTest {

    @Mock
    private FinancialRecordRepository recordRepository;

    @InjectMocks
    private FinancialRecordService recordService;

    private FinancialRecord testRecord;
    private FinancialRecordDTO.RecordRequest createRequest;
    private final Long recordId = 1L;

    @BeforeEach
    void setUp() {
        testRecord = FinancialRecord.builder()
                .id(recordId)
                .amount(new BigDecimal("250.00"))
                .type(TransactionType.EXPENSE)
                .category("Groceries")
                .date(LocalDate.of(2025, 4, 5))
                .description("Weekly groceries")
                .deleted(false)
                .build();

        createRequest = FinancialRecordDTO.RecordRequest.builder()
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.INCOME)
                .category("Salary")
                .date(LocalDate.of(2025, 4, 1))
                .description("Monthly salary")
                .build();
    }

    @Test
    void createRecord_Success() {
        when(recordRepository.save(any(FinancialRecord.class))).thenAnswer(invocation -> {
            FinancialRecord record = invocation.getArgument(0);
            record.setId(2L);
            return record;
        });

        FinancialRecord created = recordService.createRecord(createRequest);

        assertNotNull(created);
        assertEquals(2L, created.getId());
        assertEquals(createRequest.getAmount(), created.getAmount());
        assertEquals(createRequest.getType(), created.getType());
        assertEquals(createRequest.getCategory(), created.getCategory());
        assertEquals(createRequest.getDate(), created.getDate());
        assertEquals(createRequest.getDescription(), created.getDescription());
        assertFalse(created.isDeleted());

        verify(recordRepository).save(any(FinancialRecord.class));
    }

    @Test
    void updateRecord_Success() {
        when(recordRepository.findById(recordId)).thenReturn(Optional.of(testRecord));
        when(recordRepository.save(any(FinancialRecord.class))).thenReturn(testRecord);

        FinancialRecord updated = recordService.updateRecord(recordId, createRequest);

        assertEquals(createRequest.getAmount(), updated.getAmount());
        assertEquals(createRequest.getType(), updated.getType());
        assertEquals(createRequest.getCategory(), updated.getCategory());
        assertEquals(createRequest.getDate(), updated.getDate());
        assertEquals(createRequest.getDescription(), updated.getDescription());

        verify(recordRepository).findById(recordId);
        verify(recordRepository).save(testRecord);
    }

    @Test
    void updateRecord_NotFound_ThrowsResourceNotFoundException() {
        when(recordRepository.findById(recordId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> recordService.updateRecord(recordId, createRequest));
        verify(recordRepository, never()).save(any());
    }

    @Test
    void deleteRecord_Success() {
        when(recordRepository.findById(recordId)).thenReturn(Optional.of(testRecord));
        when(recordRepository.save(any(FinancialRecord.class))).thenReturn(testRecord);

        recordService.deleteRecord(recordId);

        assertTrue(testRecord.isDeleted());
        verify(recordRepository).findById(recordId);
        verify(recordRepository).save(testRecord);
    }

    @Test
    void deleteRecord_NotFound_ThrowsResourceNotFoundException() {
        when(recordRepository.findById(recordId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> recordService.deleteRecord(recordId));
        verify(recordRepository, never()).save(any());
    }

    @Test
    void getAllRecords_WithFilters_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<FinancialRecord> expectedPage = new PageImpl<>(List.of(testRecord), pageable, 1);
        when(recordRepository.findAllWithFilters(any(), any(), any(), any(), any(), eq(pageable)))
                .thenReturn(expectedPage);

        Page<FinancialRecord> result = recordService.getAllRecords(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                "Groceries", TransactionType.EXPENSE, "INCOME", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testRecord, result.getContent().get(0));
        verify(recordRepository).findAllWithFilters(any(), any(), any(), any(), any(), eq(pageable));
    }

    @Test
    void getAllRecords_NoFilters_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(recordRepository.findAllWithFilters(null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        Page<FinancialRecord> result = recordService.getAllRecords(null, null, null, null, null, pageable);
        assertTrue(result.isEmpty());
    }

    @Test
    void getSummary_WithBothIncomeAndExpense_ReturnsCorrect() {
        when(recordRepository.sumByType(TransactionType.INCOME)).thenReturn(new BigDecimal("5000"));
        when(recordRepository.sumByType(TransactionType.EXPENSE)).thenReturn(new BigDecimal("3200"));

        DashboardDTO.Summary summary = recordService.getSummary();

        assertEquals(new BigDecimal("5000"), summary.getTotalIncome());
        assertEquals(new BigDecimal("3200"), summary.getTotalExpense());
        assertEquals(new BigDecimal("1800"), summary.getNetBalance());
    }

    @Test
    void getSummary_NoIncome_ReturnsZero() {
        when(recordRepository.sumByType(TransactionType.INCOME)).thenReturn(null);
        when(recordRepository.sumByType(TransactionType.EXPENSE)).thenReturn(new BigDecimal("500"));

        DashboardDTO.Summary summary = recordService.getSummary();

        assertEquals(BigDecimal.ZERO, summary.getTotalIncome());
        assertEquals(new BigDecimal("500"), summary.getTotalExpense());
        assertEquals(new BigDecimal("-500"), summary.getNetBalance());
    }

    @Test
    void getSummary_NoExpense_ReturnsZero() {
        when(recordRepository.sumByType(TransactionType.INCOME)).thenReturn(new BigDecimal("1000"));
        when(recordRepository.sumByType(TransactionType.EXPENSE)).thenReturn(null);

        DashboardDTO.Summary summary = recordService.getSummary();

        assertEquals(new BigDecimal("1000"), summary.getTotalIncome());
        assertEquals(BigDecimal.ZERO, summary.getTotalExpense());
        assertEquals(new BigDecimal("1000"), summary.getNetBalance());
    }

    @Test
    void getSummary_NoRecords_ReturnsZeroBoth() {
        when(recordRepository.sumByType(TransactionType.INCOME)).thenReturn(null);
        when(recordRepository.sumByType(TransactionType.EXPENSE)).thenReturn(null);

        DashboardDTO.Summary summary = recordService.getSummary();

        assertEquals(BigDecimal.ZERO, summary.getTotalIncome());
        assertEquals(BigDecimal.ZERO, summary.getTotalExpense());
        assertEquals(BigDecimal.ZERO, summary.getNetBalance());
    }

    @Test
    void getCategoryTotals_ReturnsList() {
        List<Object[]> mockResults = List.of(
                new Object[]{"Groceries", new BigDecimal("150")},
                new Object[]{"Transport", new BigDecimal("50")}
        );
        when(recordRepository.sumGroupByCategory(TransactionType.EXPENSE)).thenReturn(mockResults);

        List<DashboardDTO.CategoryTotal> totals = recordService.getCategoryTotals(TransactionType.EXPENSE);

        assertEquals(2, totals.size());
        assertEquals("Groceries", totals.get(0).getCategory());
        assertEquals(new BigDecimal("150"), totals.get(0).getTotal());
        assertEquals("Transport", totals.get(1).getCategory());
        assertEquals(new BigDecimal("50"), totals.get(1).getTotal());
    }

    @Test
    void getCategoryTotals_EmptyList_ReturnsEmpty() {
        when(recordRepository.sumGroupByCategory(TransactionType.INCOME)).thenReturn(Collections.emptyList());
        List<DashboardDTO.CategoryTotal> totals = recordService.getCategoryTotals(TransactionType.INCOME);
        assertTrue(totals.isEmpty());
    }

    @Test
    void getRecentActivity_ReturnsTop5() {
        List<FinancialRecord> mockRecords = List.of(testRecord);
        when(recordRepository.findTop5ByDeletedFalseOrderByDateDescCreatedAtDesc()).thenReturn(mockRecords);

        List<FinancialRecordDTO.RecordResponse> recent = recordService.getRecentActivity();

        assertEquals(1, recent.size());
        FinancialRecordDTO.RecordResponse response = recent.get(0);
        assertEquals(testRecord.getId(), response.getId());
        assertEquals(testRecord.getAmount(), response.getAmount());
        assertEquals(testRecord.getType(), response.getType());
        assertEquals(testRecord.getCategory(), response.getCategory());
        assertEquals(testRecord.getDate(), response.getDate());
        assertEquals(testRecord.getDescription(), response.getDescription());
    }

    @Test
    void getRecentActivity_Empty_ReturnsEmpty() {
        when(recordRepository.findTop5ByDeletedFalseOrderByDateDescCreatedAtDesc()).thenReturn(Collections.emptyList());
        List<FinancialRecordDTO.RecordResponse> recent = recordService.getRecentActivity();
        assertTrue(recent.isEmpty());
    }

    @Test
    void getMonthlyTrends_ReturnsList() {
        List<Object[]> mockResults = List.of(
                new Object[]{2025, 3, new BigDecimal("1000"), new BigDecimal("300")},
                new Object[]{2025, 2, new BigDecimal("800"), new BigDecimal("200")}
        );
        when(recordRepository.findMonthlyTrends()).thenReturn(mockResults);

        List<DashboardDTO.MonthlyTrend> trends = recordService.getMonthlyTrends();

        assertEquals(2, trends.size());
        DashboardDTO.MonthlyTrend first = trends.get(0);
        assertEquals(2025, first.getYear());
        assertEquals(3, first.getMonth());
        assertEquals(new BigDecimal("1000"), first.getIncome());
        assertEquals(new BigDecimal("300"), first.getExpense());

        DashboardDTO.MonthlyTrend second = trends.get(1);
        assertEquals(2025, second.getYear());
        assertEquals(2, second.getMonth());
        assertEquals(new BigDecimal("800"), second.getIncome());
        assertEquals(new BigDecimal("200"), second.getExpense());
    }

    @Test
    void getMonthlyTrends_HandlesNullIncomeOrExpense() {
        List<Object[]> mockResults = List.of(
                new Object[]{2025, 3, null, new BigDecimal("150")},
                new Object[]{2025, 2, new BigDecimal("500"), null}
        );
        when(recordRepository.findMonthlyTrends()).thenReturn(mockResults);

        List<DashboardDTO.MonthlyTrend> trends = recordService.getMonthlyTrends();

        assertEquals(BigDecimal.ZERO, trends.get(0).getIncome());
        assertEquals(new BigDecimal("150"), trends.get(0).getExpense());
        assertEquals(new BigDecimal("500"), trends.get(1).getIncome());
        assertEquals(BigDecimal.ZERO, trends.get(1).getExpense());
    }

    @Test
    void getMonthlyTrends_Empty_ReturnsEmpty() {
        when(recordRepository.findMonthlyTrends()).thenReturn(Collections.emptyList());
        List<DashboardDTO.MonthlyTrend> trends = recordService.getMonthlyTrends();
        assertTrue(trends.isEmpty());
    }
}