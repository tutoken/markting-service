package com.monitor.constants;

import java.util.HashMap;
import java.util.Map;

public class Topics {

    // https://www.4byte.directory/signatures/

    // TODO use events


    static final Map<String, String[]> TOPICS = new HashMap<>();

    static {
        TOPICS.put("REQUEST_MINT", new String[]{"0x883eab2a74c029007e37f3f118fa7713d39b756c0b7c932a0269fcb995a4724c", null, null});
        TOPICS.put("RATIFIED_MINT", new String[]{"0x86cc1a29a55449d1229bb301da3d61fcd5490843635df9a79e5a4df4724773d2", null, null});
        TOPICS.put("MINT", new String[]{"0x0f6798a560793a54c3bcfe86a93cde1e73087d944c0ea20544137d4121396885", null});
        TOPICS.put("REDEEM", new String[]{"0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef", null, "0x0000000000000000000000000000000000000000000000000000000000000000"});
        TOPICS.put("TRANSFER", new String[]{"0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef", null, null});
    }

    private static final Map<String, String> METHOD_SIGNATURE = Map.of("0xd27d5087", "refill ratified mintPool", "0xc41a3be8", "deposit", "0xa9059cbb", "transfer", "0x0965d04b", "settle orders", "0x80749656", "set can burn", "0x76f2a59a", "instant mint", "0xcc7e492e", "refill instant mint pool");

    public static String METHOD(String method) {
        return METHOD_SIGNATURE.getOrDefault(method, method);
    }

    public static String[] TOPIC(String topic) {
        return TOPICS.get(topic);
    }
}
