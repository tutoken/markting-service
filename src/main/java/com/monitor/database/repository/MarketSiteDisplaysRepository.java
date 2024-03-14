package com.monitor.database.repository;

import com.monitor.database.model.MarketSiteDisplays;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.monitor.database.model.MarketSiteDisplays.Type;

@Repository
public interface MarketSiteDisplaysRepository extends JpaRepository<MarketSiteDisplays, Integer> {

    Optional<MarketSiteDisplays> findTopByTypeOrderByVersionDesc(Type type);
}