package com.example.finance_app.finance_app.models.dto;

import com.example.finance_app.finance_app.models.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.misc.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FinancialRecordDTO {
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordRequest {
        @NotNull
        @Positive
        private BigDecimal amount;
        @NotNull private TransactionType type;
        @NotBlank
        private String category;
        @NotNull @PastOrPresent
        private LocalDate date;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordResponse {
        private Long id;
        private BigDecimal amount;
        private TransactionType type;
        private String category;
        private LocalDate date;
        private String description;
    }
}