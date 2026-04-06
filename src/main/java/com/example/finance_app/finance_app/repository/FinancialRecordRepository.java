package com.example.finance_app.finance_app.repository;

import com.example.finance_app.finance_app.entity.FinancialRecord;
import com.example.finance_app.finance_app.models.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long> {
    Page<FinancialRecord> findAllByDeletedFalse(Pageable pageable);

  /*  @Query("SELECT r FROM FinancialRecord r WHERE r.deleted = false " +
           "AND (:startDate IS NULL OR r.date >= :startDate) " +
           "AND (:endDate IS NULL OR r.date <= :endDate) " +
           "AND (:category IS NULL OR r.category = :category) " +
           "AND (:type IS NULL OR r.type = :type)")
    Page<FinancialRecord> findAllWithFilters(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate,
                                             @Param("category") String category,
                                             @Param("type") TransactionType type,
                                             Pageable pageable);*/

    @Query("SELECT r FROM FinancialRecord r WHERE r.deleted = false " +
            "AND (:startDate IS NULL OR r.date >= :startDate) " +
            "AND (:endDate IS NULL OR r.date <= :endDate) " +
            "AND (:category IS NULL OR r.category = :category) " +
            "AND (:type IS NULL OR r.type = :type) " +
            "AND (:description IS NULL OR LOWER(r.description) LIKE LOWER(CONCAT('%', :description, '%')))")
    Page<FinancialRecord> findAllWithFilters(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate,
                                             @Param("category") String category,
                                             @Param("type") TransactionType type,
                                             @Param("description") String description,
                                             Pageable pageable);

    @Query("SELECT SUM(r.amount) FROM FinancialRecord r WHERE r.type = :type AND r.deleted = false")
    BigDecimal sumByType(@Param("type") TransactionType type);

    @Query("SELECT r.category, SUM(r.amount) FROM FinancialRecord r " +
           "WHERE r.type = :type AND r.deleted = false GROUP BY r.category")
    List<Object[]> sumGroupByCategory(@Param("type") TransactionType type);

    @Query("SELECT FUNCTION('YEAR', r.date) as year, FUNCTION('MONTH', r.date) as month, " +
           "SUM(CASE WHEN r.type = 'INCOME' THEN r.amount ELSE 0 END) as income, " +
           "SUM(CASE WHEN r.type = 'EXPENSE' THEN r.amount ELSE 0 END) as expense " +
           "FROM FinancialRecord r WHERE r.deleted = false GROUP BY YEAR(r.date), MONTH(r.date) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> findMonthlyTrends();

    List<FinancialRecord> findTop5ByDeletedFalseOrderByDateDescCreatedAtDesc();
}