package com.monitor.service.common;

import com.monitor.utils.HttpUtil;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;

import static org.mockito.Mockito.mockStatic;

@SpringBootTest
public class MarketSiteServiceTest {

    @Test
    public void testCurrentPrice() {
        try (MockedStatic<HttpUtil> ignored = mockStatic(HttpUtil.class)) {

        }
    }

//    @Test
//    public void testOverview() {
//
//    }
//
//    @Test
//    public void testEcosystem() {
//
//    }
//
//    @Test
//    public void testUpdateTotalTransactionCount() {
//
//    }
}
