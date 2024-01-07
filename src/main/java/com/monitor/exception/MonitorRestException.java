package com.monitor.exception;

import com.monitor.api.MonitorController;
import org.hibernate.service.spi.ServiceException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

//@RestControllerAdvice(basePackageClasses = {MarketSiteController.class, MonitorController.class})
@RestControllerAdvice(basePackageClasses = {MonitorController.class})
public class MonitorRestException {

    @ExceptionHandler(ServiceException.class)
    public Map<String, Object> serviceException(ServiceException e) {
        return Map.of("500", e.getMessage());
    }
}