package com.monitor.service.impl.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.monitor.database.model.DailyReport;
import com.monitor.database.repository.DailyReportRepository;
import com.monitor.service.interfaces.AWSService;
import com.monitor.service.interfaces.DailyReportService;
import com.monitor.service.interfaces.SlackService;
import com.monitor.utils.HttpUtil;
import com.monitor.utils.RedisUtil;
import com.monitor.utils.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.monitor.constants.ThirdPartyService.*;

@Service
@Slf4j
public class DailyReportServiceImpl implements DailyReportService {

    @Autowired
    private AWSService awsService;

    @Autowired
    private SlackService slackService;

    @Autowired
    private DailyReportRepository dailyReportRepository;

    @Value("${aws.bucket}")
    private String bucket;

    @Autowired
    private RedisUtil redisUtil;

    @Value("${upload.filepath}")
    private String filePath;

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
    public Map<String, Object> getDailyReport() {
        Map<String, Object> result = new HashMap<>();
        Object object = redisUtil.getObjectValue("daily_report");
        DailyReport dailyReport;
        if (object != null) {
            dailyReport = (DailyReport) object;
        } else {
            Optional<DailyReport> optional = dailyReportRepository.findFirstByOrderByIdDesc();
            if (optional.isEmpty()) {
                slackService.sendNotice("test", "Get daily report failed.");
                return null;
            }
            dailyReport = optional.get();
        }
        String tokens = dailyReport.getTokens();
        if (tokens != null) {
            result.put("token", JSONObject.parseObject(tokens, Map.class));
        }
        result.put("ripcord", dailyReport.getRipcord());

        BigDecimal totalTrust = dailyReport.getTotalTrust();
        BigDecimal totalToken = dailyReport.getTotalToken();
//        BigDecimal quotient = totalTrust.divide(totalToken, new MathContext(3, RoundingMode.HALF_UP));
        BigDecimal quotient = totalTrust.divide(totalToken, 2, RoundingMode.HALF_UP);
        BigDecimal percentage = quotient.multiply(new BigDecimal("100"));
        BigDecimal collateralRatio = (percentage.compareTo(new BigDecimal("97")) >= 0 && percentage.compareTo(new BigDecimal("103")) <= 0) ? new BigDecimal("100") : percentage;

        result.put("totalTrust", totalTrust);
        result.put("totalToken", totalToken);
        result.put("collateralRatio", collateralRatio);
        result.put("updatedAt", TimeUtil.GET_DATE_BY_STAMP(dailyReport.getUpdatedAt().getTime()));

        result.put("reportUrl", awsService.generateDownloadUrl(bucket, dailyReport.getReport_file()));

        log.info(result.toString());
        return result;
    }

    @Override
    public void createDailyReport() {
        Map<String, String> header = Map.of("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36");
        String response = HttpUtil.get(RESERVE_URL, header, 3);
        if (response == null) {
            log.error("Can not read reserves info from ledgerlens.");
            slackService.sendNotice("test", "Can not read reserves info from ledgerlens.");
            return;
        }

        JSONObject data = JSON.parseObject(response);
        DailyReport dailyReport = new DailyReport();

        dailyReport.setRipcord(data.getBoolean("ripcord") ? data.getJSONArray("ripcordDetails").getString(0) : "None");
        dailyReport.setTotalTrust(data.getBigDecimal("totalTrust").setScale(2, RoundingMode.HALF_UP));
        dailyReport.setTotalToken(data.getBigDecimal("totalToken").setScale(2, RoundingMode.HALF_UP));
        dailyReport.setUpdatedAt(data.getTimestamp("updatedAt"));

        JSONArray tokens = data.getJSONArray("token");
        if (!CollectionUtils.isEmpty(tokens)) {
            Map<String, String> tokenInfo = tokens.stream().map(obj -> (JSONObject) obj).collect(Collectors.toMap(obj -> {
                String fullTokenName = (String) obj.get("tokenName");
                return fullTokenName.substring(fullTokenName.lastIndexOf('(') + 1, fullTokenName.lastIndexOf(')'));
            }, obj -> ((BigDecimal) obj.get("totalTokenByChain")).setScale(2, RoundingMode.HALF_UP).toString()));
            dailyReport.setTokens(JSON.toJSONString(tokenInfo));
        }

        dailyReport.setReport_file(restoreReport());

        dailyReportRepository.save(dailyReport);
        redisUtil.saveObjectValue("daily_report", dailyReport);
    }

    @Override
    public String restoreReport() {
        byte[] downloadContents = HttpUtil.download(REPORT_URL, 3);
        if (downloadContents.length < 1) {
            slackService.sendNotice("test", "Download daily report failed.");
            return null;
        }
        String md5Pass = DigestUtils.md5DigestAsHex(downloadContents);
        if (!awsService.uploadFileInBytes(downloadContents, bucket, md5Pass)) {
            try (FileOutputStream outputStream = new FileOutputStream(filePath + md5Pass)) {
                outputStream.write(downloadContents);
            } catch (Exception ex) {
                log.error("Store file failed.", ex);
                slackService.sendNotice("test", "Download daily report succeeded but upload failed.");
            }
        }
        return md5Pass;
    }
}
