package com.monitor.database.repository;

import com.monitor.database.model.DailyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {

    Optional<DailyReport> findFirstByOrderByIdDesc();
}