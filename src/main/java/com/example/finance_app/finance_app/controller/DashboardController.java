package com.example.finance_app.finance_app.controller;

import com.example.finance_app.finance_app.models.TransactionType;
import com.example.finance_app.finance_app.models.dto.DashboardDTO;
import com.example.finance_app.finance_app.models.dto.FinancialRecordDTO;
import com.example.finance_app.finance_app.service.FinancialRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final FinancialRecordService recordService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<DashboardDTO.Summary> getSummary() {
        return ResponseEntity.ok(recordService.getSummary());
    }

    @GetMapping("/category-totals")
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<List<DashboardDTO.CategoryTotal>> getCategoryTotals(
            @RequestParam(defaultValue = "EXPENSE") TransactionType type) {
        return ResponseEntity.ok(recordService.getCategoryTotals(type));
    }

    @GetMapping("/recent-activity")
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<List<FinancialRecordDTO.RecordResponse>> getRecentActivity() {
        return ResponseEntity.ok(recordService.getRecentActivity());
    }

    @GetMapping("/monthly-trends")
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<List<DashboardDTO.MonthlyTrend>> getMonthlyTrends() {
        return ResponseEntity.ok(recordService.getMonthlyTrends());
    }
}