package com.monitor.api;

import com.monitor.schedule.ScheduleTaskController;
import com.monitor.service.interfaces.DailyReportService;
import com.monitor.service.interfaces.EMailService;
import com.monitor.service.interfaces.MarketSiteService;
import com.monitor.service.interfaces.Web3Service;
import com.monitor.service.parameter.CommonResponse;
import com.monitor.service.parameter.SubmitFormParam;
import com.monitor.service.parameter.TotalSupply;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@EnableSwagger2
@Api(value = "Monitor")
@Slf4j
public class MarketSiteController {
    @Autowired
    private Web3Service web3Service;

    @Autowired
    private MarketSiteService marketSiteService;

    @Autowired
    private DailyReportService dailyReportService;

    @Autowired
    private EMailService eMailService;

    @Autowired
    private ScheduleTaskController scheduleTaskController;

    @RequestMapping("/health/check")
    public String healthCheck() {
        return "OK";
    }

    @ApiOperation(value = "queryTotalSupply", httpMethod = "GET", notes = "Query the total supply at the current time")
//    @RateLimiter(time = 60, count = 100, limitType = RateLimiter.LimitType.IP)
    @RequestMapping("/totalsupply")
    public List<TotalSupply> queryTotalSupply() {
        return web3Service.getTotalSupply();
    }

    @ApiOperation(value = "totalSupply", httpMethod = "GET", notes = "Query the total supply at a given time")
    @RequestMapping("/totalSupplyByTimestamp")
    public Map<String, BigDecimal> totalSupply(@RequestParam(name = "chain") String chain, @RequestParam(name = "time") String time) {
        return web3Service.queryTotalSupplyByTimestamp(chain, time);
    }

    @ApiOperation(value = "getCurrentPrice", httpMethod = "GET", notes = "Query current price of a given token.")
    @RequestMapping("/price")
    public String price(@RequestParam(name = "symbol") String symbol, @RequestParam(name = "convert") String convert) {
        return marketSiteService.currentPrice(symbol, convert);
    }

    @ApiOperation(value = "overview", httpMethod = "GET", notes = "Get circulating supply")
    @RequestMapping("/overview")
//    @RateLimiter(time = 60, count = 100, limitType = RateLimiter.LimitType.IP)
    public Map<String, String> overview() {
        return marketSiteService.overview();
    }

    @ApiOperation(value = "ecosystem", httpMethod = "GET", notes = "Obtain transaction volume within 24 hours")
    @RequestMapping("/ecosystem")
    Map<String, Map<String, Map<String, String>>> ecosystem() {
        return marketSiteService.ecosystem();
    }

    @ApiOperation(value = "support", httpMethod = "POST", notes = "Obtain audit alarm status")
    @PostMapping("/submit/support")
    public CommonResponse submitSupport(@RequestBody SubmitFormParam submitFormParam) {
        // TODO use global exception handler
        try {
            submitFormParam.validateContact();
        } catch (Exception exception) {
            return new CommonResponse(false, exception.getMessage());
        }
        return eMailService.submit(submitFormParam.setSubject("New TrueUSD Partnership Application Form"));
    }

    @ApiOperation(value = "support", httpMethod = "POST", notes = "Obtain audit alarm status")
    @PostMapping("/submit/job")
//    @ExceptionHandler({IllegalArgumentException.class, ServiceException.class})
    public CommonResponse submitJob(@RequestBody SubmitFormParam submitFormParam) {
        try {
            submitFormParam.validateJob();
        } catch (Exception exception) {
            return new CommonResponse(false, exception.getMessage());
        }
        return eMailService.submit(submitFormParam.setSubject("Apply for Job"));
    }

    @ApiOperation(value = "upload attachment", httpMethod = "POST", notes = "Upload file")
    @PostMapping("/attachment/upload")
    public CommonResponse upload(@RequestParam("file") MultipartFile file) {
        return eMailService.upload(file);
    }

    @GetMapping("/test")
    public void test() {
    }

    @ApiOperation(value = "real-time reserve", httpMethod = "GET", notes = "get real-time reserve")
    @GetMapping("/reserves")
    public Map<String, Object> reserve() {
        return dailyReportService.getDailyReport();
    }
}
