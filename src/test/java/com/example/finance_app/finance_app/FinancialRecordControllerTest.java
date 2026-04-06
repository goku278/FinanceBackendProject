package com.example.finance_app.finance_app;


import com.example.finance_app.finance_app.config.CustomUserDetailsService;
import com.example.finance_app.finance_app.config.JwtUtil;
import com.example.finance_app.finance_app.config.SecurityConfig;
import com.example.finance_app.finance_app.controller.FinancialRecordController;
import com.example.finance_app.finance_app.entity.FinancialRecord;
import com.example.finance_app.finance_app.exceptions.GlobalExceptionHandler;
import com.example.finance_app.finance_app.models.TransactionType;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FinancialRecordController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class FinancialRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FinancialRecordService recordService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private BlacklistedTokenRepository blacklistedTokenRepository;

    private FinancialRecord testRecord;
    private FinancialRecordDTO.RecordRequest validRequest;
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

        validRequest = FinancialRecordDTO.RecordRequest.builder()
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.INCOME)
                .category("Salary")
                .date(LocalDate.of(2025, 4, 1))
                .description("Monthly salary")
                .build();
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    void getAllRecords_AsAnalyst_ReturnsPage() throws Exception {
        Page<FinancialRecord> page = new PageImpl<>(List.of(testRecord), PageRequest.of(0, 20), 1);
        when(recordService.getAllRecords(any(), any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/records")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-12-31")
                        .param("category", "Groceries")
                        .param("type", "EXPENSE")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(recordId))
                .andExpect(jsonPath("$.content[0].amount").value(250.00))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    void getAllRecords_WithoutFilters_ReturnsPage() throws Exception {
        Page<FinancialRecord> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(recordService.getAllRecords(any(), any(), any(), any(), any(), any())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getAllRecords_AsViewer_Forbidden() throws Exception {
        mockMvc.perform(get("/api/records"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRecord_AsAdmin_ReturnsCreated() throws Exception {
        when(recordService.createRecord(any(FinancialRecordDTO.RecordRequest.class))).thenReturn(testRecord);

        mockMvc.perform(post("/api/records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(recordId))
                .andExpect(jsonPath("$.amount").value(250.00))
                .andExpect(jsonPath("$.type").value("EXPENSE"));
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    void createRecord_AsAnalyst_Forbidden() throws Exception {
        mockMvc.perform(post("/api/records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRecord_InvalidRequest_ReturnsBadRequest() throws Exception {
        FinancialRecordDTO.RecordRequest invalidRequest = FinancialRecordDTO.RecordRequest.builder()
                .amount(null)
                .type(null)
                .category("")
                .date(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.category").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateRecord_AsAdmin_ReturnsOk() throws Exception {
        when(recordService.updateRecord(eq(recordId), any(FinancialRecordDTO.RecordRequest.class))).thenReturn(testRecord);

        mockMvc.perform(put("/api/records/{id}", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(recordId));
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    void updateRecord_AsAnalyst_Forbidden() throws Exception {
        mockMvc.perform(put("/api/records/{id}", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateRecord_InvalidRequest_ReturnsBadRequest() throws Exception {
        FinancialRecordDTO.RecordRequest invalid = FinancialRecordDTO.RecordRequest.builder()
                .amount(new BigDecimal("-10"))
                .type(TransactionType.EXPENSE)
                .category("Valid")
                .date(LocalDate.now())
                .build();

        mockMvc.perform(put("/api/records/{id}", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.amount").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteRecord_AsAdmin_ReturnsNoContent() throws Exception {
        doNothing().when(recordService).deleteRecord(recordId);

        mockMvc.perform(delete("/api/records/{id}", recordId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    void deleteRecord_AsAnalyst_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/records/{id}", recordId))
                .andExpect(status().isForbidden());
    }
}