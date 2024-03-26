package com.monitor.service.impl.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.monitor.constants.Monitor;
import com.monitor.constants.Slack;
import com.monitor.constants.Token.TUSD;
import com.monitor.constants.Web3Provider;
import com.monitor.database.model.MarketSiteDisplays;
import com.monitor.database.repository.EcosystemRepository;
import com.monitor.database.repository.MarketSiteDisplaysRepository;
import com.monitor.service.ServiceContext;
import com.monitor.service.interfaces.MarketSiteService;
import com.monitor.service.interfaces.SlackService;
import com.monitor.service.interfaces.Web3Service;
import com.monitor.service.parameter.Announcement;
import com.monitor.utils.HttpUtil;
import com.monitor.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.monitor.constants.ThirdPartyService.*;
import static com.monitor.database.model.MarketSiteDisplays.Type.announcement;

/**
 * All externally provided interfaces
 */
@Slf4j
@Service
public class MarketSiteServiceImpl implements MarketSiteService {

    @Autowired
    private Slack slack;

    @Autowired
    private Monitor monitor;

    @Autowired
    private TUSD token;

    @Autowired
    private Web3Provider web3Provider;

    @Autowired
    private SlackService slackService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ServiceContext serviceContext;

    @Autowired
    private Web3Service web3Service;

    @Autowired
    private EcosystemRepository ecosystemRepository;

    @Autowired
    private MarketSiteDisplaysRepository marketSiteDisplaysRepository;

    @Value("${upload.filepath}")
    private String filePath;

    // TODO move url to properties
    public String currentPrice(String symbol, String convert) {
        try {
            String baseUrl = COIN_MARKET_CURRENCY;
            String url = baseUrl + "?symbol=" + symbol + "&convert=" + convert;
            Map<String, String> properties = Map.of("Accepts", "application/json", "X-CMC_PRO_API_KEY", "ec4fa893-3f2d-4a09-8ede-7b81253a72cf");

            String response = HttpUtil.get(url, properties, 3);

            if (response == null) {
                return "N/A";
            }
            JSONObject data = JSON.parseObject(response);
            Object tusdInfo = data.getJSONObject("data").getJSONArray(symbol).stream().filter(a -> ((JSONObject) a).getInteger("id") == 2563).findFirst().get();

            return ((JSONObject) tusdInfo).getJSONObject("quote").getJSONObject(convert).getString("price");
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return "0";
    }

    @Override
    public Map<String, String> overview() {
        String circulatingSupply = redisUtil.getStringValueOrDefault("lastCirculatingSupply", this.circulatingSupply());
        String tradingVolume = redisUtil.getStringValueOrDefault("lastTradingVolume", this.tradingVolume());
        String totalTransactions = redisUtil.getStringValueOrDefault("totalTransactions", "0");

        return Map.of("circulatingSupply", circulatingSupply, "tradingVolume", tradingVolume, "totalSupportChain", "10", "totalTransactions", totalTransactions);
    }

    private String circulatingSupply() {
        String circulatingSupply;
        try {
            Map<String, BigDecimal> totalSupplies = web3Service.queryTotalSupplyWithSummary();
            circulatingSupply = totalSupplies.get("Natively Supply").toString();

            redisUtil.saveStringValue("circulatingSupply", circulatingSupply, 0, null);
            redisUtil.saveStringValue("lastCirculatingSupply", circulatingSupply, 20, TimeUnit.MINUTES);

            return circulatingSupply;

        } catch (Exception exception) {
            log.error("Get circulating supply failed.", exception);
            return redisUtil.getStringValueOrDefault("circulatingSupply", "2573337233");
        }
    }

    private String tradingVolume() {
        try {
            String url = COIN_MARKET_TRADING_VOLUME;
            Map<String, String> properties = Map.of("Accepts", "application/json", "X-CMC_PRO_API_KEY", "ec4fa893-3f2d-4a09-8ede-7b81253a72cf");

            String response = HttpUtil.get(url, properties, 3);

            if (response == null) {
                log.error("Call coinmarketcap failed");
                return redisUtil.getStringValueOrDefault("tradingVolume", "2573337233");
            }
            JSONObject data = JSON.parseObject(response);
            Object tusdInfo = data.getJSONArray("data").stream().filter(a -> ((JSONObject) a).getString("symbol").equals("TUSD")).findFirst().get();

            String tradingVolume = ((JSONObject) tusdInfo).getJSONObject("quote").getJSONObject("USD").getString("volume_24h");

            redisUtil.saveStringValue("tradingVolume", tradingVolume, 0, null);
            redisUtil.saveStringValue("lastTradingVolume", tradingVolume, 20, TimeUnit.MINUTES);

            return tradingVolume;
        } catch (Exception ex) {
            log.error("Get trading volume failed.", ex);

            return redisUtil.getStringValueOrDefault("tradingVolume", "2573337233");
        }
    }

    private String getTotalTransactionCount() {
        int result = 0;

        try {
            for (String chain : monitor.getChains()) {
                if ("aurora".equals(chain) || "heco".equals(chain)) {
                    continue;
                }
                int count = serviceContext.chainServiceOf(chain).getTransactionCount();
                result += count;
            }
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            return redisUtil.getStringValueOrDefault("totalTransactions", "3476631");
        }

        redisUtil.saveStringValue("totalTransactions", String.valueOf(result), 0, null);

        return String.valueOf(result);
    }


    @Override
    public Map<String, Map<String, Map<String, String>>> ecosystem() {

        List<Object[]> results = ecosystemRepository.getEcosystemDetails();

        Map<String, Map<String, Map<String, String>>> ecosystemResults = new LinkedHashMap<>();
        for (Object[] result : results) {
            String category = (String) result[0];
            String name = (String) result[1];
            String field = (String) result[2];
            String value = (String) result[3];

            if (!ecosystemResults.containsKey(category)) {
                ecosystemResults.put(category, new LinkedHashMap<>());
            }
            if (!ecosystemResults.get(category).containsKey(name)) {
                ecosystemResults.get(category).put(name, new LinkedHashMap<>());
            }
            ecosystemResults.get(category).get(name).put(field, value);
        }

        return ecosystemResults;
    }

    @Override
    public void updateTotalTransactionCount() {
        getTotalTransactionCount();
    }

    @Override
    public void uploadFile(MultipartFile file) {
        File tempFile = new File(filePath + file.getOriginalFilename());
        try {
            file.transferTo(tempFile);
        } catch (IOException e) {
            log.error("Upload file failed.", e);
        }
    }

    @Override
    public String ripcords() {
        String response = HttpUtil.get(RIPCORD_URL);
        if (response == null) {
            return null;
        }

        JSONArray data = JSON.parseArray(response);

        if (data == null) {
            return null;
        }

        Optional<Object> optional = data.stream().filter(obj -> ((JSONObject) obj).getString("accountName").equals("TrueUSD")).findFirst();

        if (optional.isPresent()) {
            JSONObject ripcords = ((JSONObject) optional.get()).getJSONObject("ripcords");
            if (ripcords != null) {
                JSONArray status = ripcords.getJSONArray("status");
                if (!CollectionUtils.isEmpty(status)) {
                    return status.getString(0);
                }
                return null;
            }
        }

        return null;
    }

    @Override
    public Announcement getAnnouncement() {
        MarketSiteDisplays marketSiteDisplays = marketSiteDisplaysRepository.findTopByTypeOrderByVersionDesc(announcement)
                .orElseGet(() -> {
                    log.warn("No MarketSiteDisplays found for announcement.");
                    return new MarketSiteDisplays();
                });

        return new Announcement(marketSiteDisplays.getContent(), marketSiteDisplays.isEnable());
    }
}
