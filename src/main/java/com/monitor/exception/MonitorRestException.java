package com.monitor.exception;

import com.monitor.api.ContractController;
import com.monitor.api.MarketSiteController;
import org.hibernate.service.spi.ServiceException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackageClasses = {MarketSiteController.class, ContractController.class})
public class MonitorRestException {

    @ExceptionHandler(ServiceException.class)
    public Map<String, Object> serviceException(ServiceException e) {
        return Map.of("500", e.getMessage());
    }
}