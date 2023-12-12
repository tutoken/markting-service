package com.monitor.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;

public class CommonUtil {
    private static final NumberFormat FORMATTER = NumberFormat.getInstance();

    static {
        FORMATTER.setGroupingUsed(true);
    }

    public static String FORMAT(String number) {
        return number.equals("N/A") ? number : FORMATTER.format(new BigDecimal(number));
    }

    public static String sum(List<String> nums) {
        BigDecimal result = BigDecimal.ZERO;
        for (String num : nums) {
            result = result.add(new BigDecimal(num.replaceAll(",", "")));
        }
        return FORMAT(result.toString());
    }

    public static String GET_AMOUNT_VALUE(BigDecimal in, String val, int scale) {
        return String.valueOf(in.divide(new BigDecimal(val), scale, RoundingMode.HALF_UP));
    }
}
