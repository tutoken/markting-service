package com.monitor.service.common;

import com.monitor.database.model.DailyReport;
import com.monitor.database.repository.DailyReportRepository;
import com.monitor.service.interfaces.AWSService;
import com.monitor.service.interfaces.DailyReportService;
import com.monitor.service.interfaces.SlackService;
import com.monitor.utils.HttpUtil;
import com.monitor.utils.RedisUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;

import static com.monitor.constants.ResponseObjects.REALTIME_RESERVE;
import static com.monitor.constants.ResponseObjects.RIPCORD_RESULT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
public class DailyReportServiceTest {

    //    @Spy
    @Autowired
    private DailyReportService dailyReportService;

    @MockBean
    private AWSService awsService;

    @MockBean
    private RedisUtil redisUtil;

    @MockBean
    private DailyReportRepository dailyReportRepository;

    @MockBean
    private SlackService slackService;

    @Test
    public void testRipcord() {
        try (MockedStatic<HttpUtil> ignored = mockStatic(HttpUtil.class)) {

            when(HttpUtil.get(any(String.class))).thenReturn(null);
            String result = dailyReportService.ripcords();

            assertNull(result);

            when(HttpUtil.get(any(String.class))).thenReturn(RIPCORD_RESULT);
            result = dailyReportService.ripcords();

            assertEquals("Management", result);
        }
    }

    @Test
    @ExtendWith(MockitoExtension.class)
    public void testGetDailyReport() {

        try (MockedStatic<HttpUtil> ignored = mockStatic(HttpUtil.class)) {
            when(HttpUtil.get(any(String.class))).thenReturn(null);

            DailyReport dailyReport = new DailyReport();
            dailyReport.setId(14L);
            dailyReport.setTotalToken(new BigDecimal("1"));
            dailyReport.setTotalTrust(new BigDecimal("1"));
            dailyReport.setUpdatedAt(new Timestamp(1702178719000L));
            dailyReport.setTokens("{\"AVAX\":\"2984455.56\",\"BSC\":\"30130363.88\",\"BNB\":\"145015.87\",\"ETH\":\"375334358.37\",\"TRON\":\"2224383136.06\"}");
            dailyReport.setRipcord("Balances");
            dailyReport.setReport_file("8a0366ee2e720e1d362b60d2224b1787");

            String expectResult = "{totalToken=1, ripcord=Balances, reportUrl=url, totalTrust=1, token={AVAX=2984455.56, BSC=30130363.88, BNB=145015.87, ETH=375334358.37, TRON=2224383136.06}, collateralRatio=100, updatedAt=2023-12-10 03:25:19}";

            when(awsService.generateDownloadUrl(any(String.class), any(String.class))).thenReturn("url");

            when(dailyReportRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(dailyReport));
            when(redisUtil.getObjectValue(any(String.class))).thenReturn(dailyReport);

            Map<String, Object> result = dailyReportService.getDailyReport();

            assertEquals(expectResult, result.toString());
            verifyNoInteractions(dailyReportRepository);

            when(redisUtil.getObjectValue(any(String.class))).thenReturn(null);

            result = dailyReportService.getDailyReport();

            assertEquals(expectResult, result.toString());
            verify(dailyReportRepository, times(1)).findFirstByOrderByIdDesc();
        }
    }

    @Test
    public void testCreateDailyReport() {
        try (MockedStatic<HttpUtil> ignored = mockStatic(HttpUtil.class)) {
            when(HttpUtil.get(anyString(), anyMap(), anyInt())).thenReturn(null);
            dailyReportService.createDailyReport();
            doNothing().when(slackService).sendNotice(anyString(), anyString());
            verify(slackService, times(1)).sendNotice(anyString(), anyString());

            when(HttpUtil.get(anyString(), anyMap(), anyInt())).thenReturn(REALTIME_RESERVE);
            when(HttpUtil.download(anyString(), anyInt())).thenReturn(new byte[0]);
//            when(dailyReportService.restoreReport()).thenReturn("md5");
            dailyReportService.createDailyReport();

            doNothing().when(redisUtil).saveObjectValue(anyString(), any(Object.class));
            verify(redisUtil, times(1)).saveObjectValue(anyString(), any(Object.class));
        }
    }

    @Test
    public void testRestoreReport() {
        try (MockedStatic<HttpUtil> ignored = mockStatic(HttpUtil.class); FileOutputStream fileOutputStream = Mockito.mock(FileOutputStream.class)) {

            when(HttpUtil.download(anyString(), anyInt())).thenReturn(new byte[0]);
            doNothing().when(slackService).sendNotice(anyString(), anyString());
            dailyReportService.restoreReport();
            verify(slackService, times(1)).sendNotice(anyString(), anyString());

            when(HttpUtil.download(anyString(), anyInt())).thenReturn(new byte[]{10});
            when(awsService.uploadFileInBytes(any(), anyString(), anyString())).thenReturn(true);
            doNothing().when(fileOutputStream).write(any());
            String result = dailyReportService.restoreReport();

            assertEquals("68b329da9893e34099c7d8ad5cb9c940", result);
        } catch (IOException e) {
            verify(slackService, times(2)).sendNotice(anyString(), anyString());
        }
    }
}
