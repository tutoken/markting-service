package com.monitor.database.repository;

import com.monitor.database.model.SchedulerJobDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchedulerJobDetailRepository extends JpaRepository<SchedulerJobDetail, Long> {

    List<SchedulerJobDetail> findByGroupIdOrderByPriority(long groupId);

    List<SchedulerJobDetail> findByDefinitionIn(List<String> definitions);
}