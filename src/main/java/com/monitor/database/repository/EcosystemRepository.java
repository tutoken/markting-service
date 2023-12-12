package com.monitor.database.repository;

import com.monitor.database.model.EcosystemItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EcosystemRepository extends JpaRepository<EcosystemItem, Integer> {
    @Query("SELECT ei.category, ei.name, ed.id.field, ed.value FROM EcosystemItem ei JOIN EcosystemDetail ed ON ei.id = ed.id.itemId WHERE ei.status = 0 and ed.status = 0 ORDER BY ei.category, ei.order")
    List<Object[]> getEcosystemDetails();
}
