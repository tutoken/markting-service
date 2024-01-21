package com.monitor.database.repository;

import com.monitor.database.model.SchedulerJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchedulerJobRepository extends JpaRepository<SchedulerJob, Long> {

}