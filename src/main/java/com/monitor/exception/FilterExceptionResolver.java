package com.monitor.exception;


import com.alibaba.fastjson.JSON;
import com.monitor.service.interfaces.SlackService;
import com.monitor.service.parameter.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Slf4j
public class FilterExceptionResolver implements HandlerExceptionResolver {

    @Autowired
    private SlackService slackService;

    @Override
    public ModelAndView resolveException(@NotNull HttpServletRequest httpServletRequest,
                                         @NotNull HttpServletResponse httpServletResponse, Object o, Exception e) {
        httpServletResponse.setContentType("application/json;charset=UTF-8");
        httpServletResponse.setCharacterEncoding("UTF-8");
        httpServletResponse.setStatus(HttpStatus.OK.value());
        slackService.sendNotice("test", getRequestIp() + " has suspicious request.");
        try {
            httpServletResponse.getWriter().append(JSON.toJSONString(new CommonResponse(false, e.getMessage()))).flush();
        } catch (IOException ex) {
            log.error("Solve filter exception failed. ", ex);
            throw new RuntimeException(e);
        }
        return null;
    }

    private String getRequestIp() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getHeader("x-forwarded-for");
        } catch (Exception exception) {
            return "Unknown";
        }
    }
}