package com.monitor.schedule.definition;

import com.monitor.service.interfaces.TUSDService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import static com.monitor.constants.Monitor.DECIMAL18;
import static com.monitor.utils.CommonUtil.FORMAT;

@Service("MonitorMintPoolJob")
@Slf4j
public class MonitorMintPoolJob extends ScheduleJobDefinition {
    @Override
    public void run() {
        this.start();

        slackService.init();

        for (String chain : monitor.getNativelyChains()) {
            log.info(String.format("monitor %s", chain));
            Map<String, String> map = new HashMap<>();
            TUSDService tusdService = serviceContext.tusdServiceOf(chain);

            this.queryPool(map, tusdService, chain, "ratifiedMintPool", "ratifiedMintLimit");
            this.queryPool(map, tusdService, chain, "multiSigMintPool", "multiSigMintLimit");
            this.queryPool(map, tusdService, chain, "instantMintPool", "instantMintLimit");

            slackService.addMessage(chain, map);
        }

        slackService.sendAsTable("tusd");

        this.end();
    }

    private void queryPool(Map<String, String> map, TUSDService tusdService, String chain, String t1, String t2) {
        String v1 = convert(chain, tusdService.queryController(chain, t1));
        String v2 = convert(chain, tusdService.queryController(chain, t2));
        if ("N/A".equals(v1) || "N/A".equals(v2)) {
            map.put(t1, FORMAT(v1));
        } else {
            map.put(t1, FORMAT(v1) + "(" + (new BigDecimal(v1).divide(new BigDecimal(v2), 2, RoundingMode.HALF_UP)) + ")");
        }
    }

    private String convert(String chain, String value) {
        if (value == null || "".equals(value)) {
            return "N/A";
        }
        return chain.equals("tron") ? new BigDecimal(new BigInteger(value, 16)).divide(DECIMAL18, 2, RoundingMode.HALF_UP).toString() : value;
    }

}
