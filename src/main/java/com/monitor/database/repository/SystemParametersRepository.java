package com.monitor.database.repository;

import com.monitor.database.model.SystemParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemParametersRepository extends JpaRepository<SystemParameter, Long> {

    List<SystemParameter> findByType(String type);
}