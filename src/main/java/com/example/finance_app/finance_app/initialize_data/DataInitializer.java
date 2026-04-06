package com.example.finance_app.finance_app.initialize_data;

import com.example.finance_app.finance_app.entity.FinancialRecord;
import com.example.finance_app.finance_app.entity.User;
import com.example.finance_app.finance_app.models.TransactionType;
import com.example.finance_app.finance_app.models.UserRole;
import com.example.finance_app.finance_app.repository.FinancialRecordRepository;
import com.example.finance_app.finance_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FinancialRecordRepository recordRepository;

    @Override
    public void run(String... args) throws Exception {
        // Create admin user if not exists
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@finance.com")
                    .active(true)
                    .roles(Set.of(UserRole.ROLE_ADMIN))
                    .build();
            userRepository.save(admin);
            System.out.println("Admin user created: admin / admin123");
        }

        // Create sample financial records if none exist
        if (recordRepository.count() == 0) {
            recordRepository.save(FinancialRecord.builder()
                    .amount(new BigDecimal("2500.00"))
                    .type(TransactionType.INCOME)
                    .category("Salary")
                    .date(LocalDate.of(2025, 3, 1))
                    .description("Monthly salary")
                    .build());
            recordRepository.save(FinancialRecord.builder()
                    .amount(new BigDecimal("150.00"))
                    .type(TransactionType.EXPENSE)
                    .category("Groceries")
                    .date(LocalDate.of(2025, 3, 5))
                    .description("Supermarket")
                    .build());
            recordRepository.save(FinancialRecord.builder()
                    .amount(new BigDecimal("80.00"))
                    .type(TransactionType.EXPENSE)
                    .category("Entertainment")
                    .date(LocalDate.of(2025, 3, 10))
                    .description("Movie tickets")
                    .build());
            recordRepository.save(FinancialRecord.builder()
                    .amount(new BigDecimal("3000.00"))
                    .type(TransactionType.INCOME)
                    .category("Freelance")
                    .date(LocalDate.of(2025, 2, 15))
                    .description("Project payment")
                    .build());
            recordRepository.save(FinancialRecord.builder()
                    .amount(new BigDecimal("200.00"))
                    .type(TransactionType.EXPENSE)
                    .category("Utilities")
                    .date(LocalDate.of(2025, 2, 20))
                    .description("Electricity bill")
                    .build());
            System.out.println("Sample financial records created.");
        }
    }
}
