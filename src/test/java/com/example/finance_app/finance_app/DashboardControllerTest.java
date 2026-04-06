package com.example.finance_app.finance_app;

import com.example.finance_app.finance_app.config.CustomUserDetailsService;
import com.example.finance_app.finance_app.config.JwtUtil;
import com.example.finance_app.finance_app.config.SecurityConfig;
import com.example.finance_app.finance_app.controller.DashboardController;
import com.example.finance_app.finance_app.exceptions.GlobalExceptionHandler;
import com.example.finance_app.finance_app.models.TransactionType;
import com.example.finance_app.finance_app.models.dto.DashboardDTO;
import com.example.finance_app.finance_app.models.dto.FinancialRecordDTO;
import com.example.finance_app.finance_app.repository.BlacklistedTokenRepository;
import com.example.finance_app.finance_app.service.FinancialRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FinancialRecordService recordService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private BlacklistedTokenRepository blacklistedTokenRepository;

    private DashboardDTO.Summary summary;
    private List<DashboardDTO.CategoryTotal> categoryTotals;
    private List<FinancialRecordDTO.RecordResponse> recentActivity;
    private List<DashboardDTO.MonthlyTrend> monthlyTrends;

    @BeforeEach
    void setUp() {
        summary = DashboardDTO.Summary.builder()
                .totalIncome(new BigDecimal("5500.00"))
                .totalExpense(new BigDecimal("430.00"))
                .netBalance(new BigDecimal("5070.00"))
                .build();

        categoryTotals = List.of(
                DashboardDTO.CategoryTotal.builder().category("Groceries").total(new BigDecimal("150.00")).build(),
                DashboardDTO.CategoryTotal.builder().category("Entertainment").total(new BigDecimal("80.00")).build()
        );

        recentActivity = List.of(
                FinancialRecordDTO.RecordResponse.builder()
                        .id(1L).amount(new BigDecimal("150.00")).type(TransactionType.EXPENSE)
                        .category("Groceries").date(LocalDate.of(2025, 3, 5)).description("Supermarket").build(),
                FinancialRecordDTO.RecordResponse.builder()
                        .id(2L).amount(new BigDecimal("2500.00")).type(TransactionType.INCOME)
                        .category("Salary").date(LocalDate.of(2025, 3, 1)).description("Monthly salary").build()
        );

        monthlyTrends = List.of(
                DashboardDTO.MonthlyTrend.builder().year(2025).month(3).income(new BigDecimal("2500.00")).expense(new BigDecimal("230.00")).build(),
                DashboardDTO.MonthlyTrend.builder().year(2025).month(2).income(new BigDecimal("3000.00")).expense(new BigDecimal("200.00")).build()
        );
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getSummary_AsViewer_ReturnsSummary() throws Exception {
        when(recordService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome", is(5500.00)))
                .andExpect(jsonPath("$.totalExpense", is(430.00)))
                .andExpect(jsonPath("$.netBalance", is(5070.00)));
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    void getSummary_AsAnalyst_ReturnsSummary() throws Exception {
        when(recordService.getSummary()).thenReturn(summary);
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getSummary_AsAdmin_ReturnsSummary() throws Exception {
        when(recordService.getSummary()).thenReturn(summary);
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getCategoryTotals_DefaultTypeExpense() throws Exception {
        when(recordService.getCategoryTotals(TransactionType.EXPENSE)).thenReturn(categoryTotals);

        mockMvc.perform(get("/api/dashboard/category-totals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].category", is("Groceries")))
                .andExpect(jsonPath("$[0].total", is(150.00)))
                .andExpect(jsonPath("$[1].category", is("Entertainment")));
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    void getCategoryTotals_WithTypeIncome() throws Exception {
        List<DashboardDTO.CategoryTotal> incomeTotals = List.of(
                DashboardDTO.CategoryTotal.builder().category("Salary").total(new BigDecimal("5000")).build()
        );
        when(recordService.getCategoryTotals(TransactionType.INCOME)).thenReturn(incomeTotals);

        mockMvc.perform(get("/api/dashboard/category-totals")
                        .param("type", "INCOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].category", is("Salary")));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getRecentActivity_ReturnsList() throws Exception {
        when(recordService.getRecentActivity()).thenReturn(recentActivity);

        mockMvc.perform(get("/api/dashboard/recent-activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].amount", is(150.00)))
                .andExpect(jsonPath("$[0].type", is("EXPENSE")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].amount", is(2500.00)))
                .andExpect(jsonPath("$[1].type", is("INCOME")));
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    void getRecentActivity_EmptyList_ReturnsEmpty() throws Exception {
        when(recordService.getRecentActivity()).thenReturn(List.of());
        mockMvc.perform(get("/api/dashboard/recent-activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getMonthlyTrends_ReturnsList() throws Exception {
        when(recordService.getMonthlyTrends()).thenReturn(monthlyTrends);

        mockMvc.perform(get("/api/dashboard/monthly-trends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].year", is(2025)))
                .andExpect(jsonPath("$[0].month", is(3)))
                .andExpect(jsonPath("$[0].income", is(2500.00)))
                .andExpect(jsonPath("$[0].expense", is(230.00)))
                .andExpect(jsonPath("$[1].year", is(2025)))
                .andExpect(jsonPath("$[1].month", is(2)))
                .andExpect(jsonPath("$[1].income", is(3000.00)))
                .andExpect(jsonPath("$[1].expense", is(200.00)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getMonthlyTrends_EmptyList_ReturnsEmpty() throws Exception {
        when(recordService.getMonthlyTrends()).thenReturn(List.of());
        mockMvc.perform(get("/api/dashboard/monthly-trends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}