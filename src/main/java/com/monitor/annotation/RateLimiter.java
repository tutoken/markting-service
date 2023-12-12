package com.monitor.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {

    /**
     * default key
     */
    String key() default "rate_limit:";

    /**
     * rate limit period
     */
    int time() default 60;

    /**
     * rate limit time
     */
    int count() default 100;

    /**
     * limit type
     */
    LimitType limitType() default LimitType.DEFAULT;

    enum LimitType {
        /**
         * default global
         */
        DEFAULT,
        /**
         * by ip
         */
        IP
    }
}