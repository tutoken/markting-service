package com.monitor.api;

import com.monitor.schedule.ScheduleTaskController;
import com.monitor.service.ServiceContext;
import com.monitor.service.interfaces.SlackService;
import com.monitor.service.interfaces.TokenService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Class.forName;

@RestController
@EnableSwagger2
@Api(value = "Monitor")
@Slf4j
public class MonitorController {

    @Autowired
    private ServiceContext serviceContext;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ScheduleTaskController scheduleTaskController;

    @Autowired
    private SlackService slackService;

    @ApiOperation(value = "queryContract", httpMethod = "GET", notes = "Read the value of the corresponding field from the contract", response = Map.class)
    @RequestMapping("/contract")
    public Map<String, String> queryContract(@RequestParam(name = "chain") String chain, @RequestParam(name = "field") String field) {
        return Collections.singletonMap(chain, serviceContext.tusdServiceOf(chain).queryContract(chain, field));
    }

    @ApiOperation(value = "queryController", httpMethod = "GET", notes = "Read the value of the corresponding attribute from the token controller")
    @RequestMapping("/controller")
    public Map<String, String> queryController(@RequestParam(name = "chain") String chain, @RequestParam(name = "field") String field) {
        return Collections.singletonMap(chain, serviceContext.tusdServiceOf(chain).queryController(chain, field));
    }

    /**
     * [
     * {
     * "type": "String",
     * "value": "0x860822cac26fb7e74e2cfad2642bc8a14d512270"
     * },
     * {
     * "type": "String",
     * "value": "0x860822cac26fb7e74e2cfad2642bc8a14d512270"
     * }
     * ]
     *
     * @param chain
     * @param field
     * @param parameters
     * @return
     */
    @ApiOperation(value = "callContract", httpMethod = "POST", notes = "Call the specified method of the contract and obtain the return value")
    @PostMapping("/call/contract/{chain}/{field}")
    public String call(@PathVariable String chain, @PathVariable String field, @RequestBody List<Map<String, String>> parameters) throws ClassNotFoundException {
        Map<Class, Object> map = new HashMap<>();
        for (Map<String, String> parameter : parameters) {
            map.put(forName(parameter.get("type")), parameter.get("value"));
        }
        return serviceContext.tusdServiceOf(chain).funcContract(chain, field, map);
    }

    @ApiOperation(value = "totalSupply", httpMethod = "GET", notes = "Query the total supply at a given time")
    @RequestMapping("/totalSupply")
    public Map<String, BigDecimal> totalSupply(@RequestParam(name = "chain", required = false) String chain, @RequestParam(name = "time", required = false) String time) {
        return tokenService.queryTotalSupplyByTimestamp(chain, time);
    }

    @ApiOperation(value = "getCurrentPrice", httpMethod = "GET", notes = "Query current price of a given token.")
    @RequestMapping("/price")
    public String price(@RequestParam(name = "symbol") String symbol, @RequestParam(name = "convert") String convert) {
        return tokenService.currentPrice(symbol, convert);
    }

    // http://example.com/api/items/1/2/3
    @ApiOperation(value = "executeJobs", httpMethod = "GET", notes = "Query the total supply at a given time")
    @RequestMapping("/execute/{jobs}")
    public void test(@PathVariable List<String> jobs) {
        scheduleTaskController.execute(jobs);
    }

    @ApiOperation(value = "flush", httpMethod = "GET", notes = "Query the total supply at a given time")
    @RequestMapping("/slack/flush")
    public void flush() {
        slackService.flush();
    }
}