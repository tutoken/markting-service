package com.monitor.schedule.definition;

import com.monitor.service.interfaces.TUSDService;
import com.monitor.service.parameter.Message;
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

    private static final String[] title = new String[]{"chain", "ratifiedMintPool", "multiSigMintPool", "instantMintPool"};

    @Override
    public void run() {
        Message message = new Message();
        Map<String, Map<String, String>> table = new HashMap<>();

        for (String chain : monitor.getNativelyChains()) {
            if ("bnb".equals(chain)) {
                continue;
            }
            Map<String, String> map = new HashMap<>();

            log.info(String.format("monitor %s", chain));
            TUSDService tusdService = serviceContext.tusdServiceOf(chain);

            map.putAll(this.queryPool(tusdService, chain, "ratifiedMintPool", "ratifiedMintLimit"));
            map.putAll(this.queryPool(tusdService, chain, "multiSigMintPool", "multiSigMintLimit"));
            map.putAll(this.queryPool(tusdService, chain, "instantMintPool", "instantMintLimit"));

            table.put(chain, map);
        }

        message.addTable(title, table);

        slackService.sendMessage("tusd", message);
    }

    private Map<String, String> queryPool(TUSDService tusdService, String chain, String t1, String t2) {
        Map<String, String> map = new HashMap<>();

        String v1 = convert(chain, tusdService.queryController(chain, t1));
        String v2 = convert(chain, tusdService.queryController(chain, t2));
        if ("N/A".equals(v1) || "N/A".equals(v2)) {
            map.put(t1, FORMAT(v1));
        } else {
            map.put(t1, FORMAT(v1) + "(" + (new BigDecimal(v1).divide(new BigDecimal(v2), 2, RoundingMode.HALF_UP)) + ")");
        }

        return map;
    }

    private String convert(String chain, String value) {
        if (value == null || "".equals(value)) {
            return "N/A";
        }
        return chain.equals("tron") ? new BigDecimal(new BigInteger(value, 16)).divide(DECIMAL18, 2, RoundingMode.HALF_UP).toString() : value;
    }

}
