package com.monitor.configuration;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.monitor.exception.FilterExceptionResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.FormContentFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Objects;

@Slf4j
public class XSSFilter extends FormContentFilter {

    @Override
    public void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain chain) throws IOException, ServletException {

        ServletContext context = Objects.requireNonNull(this.getFilterConfig()).getServletContext();
        ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
        FilterExceptionResolver filterExceptionResolver = Objects.requireNonNull(ctx).getBean(FilterExceptionResolver.class);

        try {
            ServletRequest wrapper = new XSSHttpServletRequestWrapper(request);
            chain.doFilter(wrapper, response);

        } catch (IllegalArgumentException illegalArgumentException) {
            filterExceptionResolver.resolveException(request, response, null, illegalArgumentException);
        }
    }

    public static class XSSHttpServletRequestWrapper extends HttpServletRequestWrapper {

        private final Safelist whitelist = createWhitelist();

        private final Document.OutputSettings outputSettings = new Document.OutputSettings().prettyPrint(false);

        private byte[] requestBody;

        private Charset charSet;

        private Safelist createWhitelist() {
            return Safelist.basic();
        }

        public XSSHttpServletRequestWrapper(HttpServletRequest request) {
            super(request);

            try {
                String requestBodyStr = getRequestPostStr(request);
                if (StringUtils.isNotBlank(requestBodyStr)) {
                    this.filter(JSONObject.parseObject(requestBodyStr));
                    requestBody = requestBodyStr.getBytes(charSet);
                } else {
                    requestBody = new byte[0];
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void filter(JSONObject json) {
            for (String key : json.keySet()) {
                Object value = json.get(key);
                if (value instanceof JSONObject) {
                    filter((JSONObject) value);
                } else if (value instanceof JSONArray) {
                    JSONArray array = (JSONArray) value;
                    for (Object obj : array) {
                        if (obj instanceof JSONObject) {
                            filter((JSONObject) obj);
                        }
                    }
                } else {
                    String cleanedValue = Jsoup.clean(String.valueOf(value), "", whitelist, outputSettings).trim();
                    if (!value.equals(cleanedValue)) {
                        throw new IllegalArgumentException(String.format("%s contains illegal character!", key));
                    }
                }
            }
        }

        public String getRequestPostStr(HttpServletRequest request) throws IOException {
            String charSetStr = request.getCharacterEncoding();
            if (charSetStr == null) {
                charSetStr = "UTF-8";
            }
            charSet = Charset.forName(charSetStr);

            return StreamUtils.copyToString(request.getInputStream(), charSet);
        }

        @Override
        public ServletInputStream getInputStream() {
            if (requestBody == null) {
                requestBody = new byte[0];
            }
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(requestBody);

            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return false;
                }

                @Override
                public boolean isReady() {
                    return false;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }

                @Override
                public int read() {
                    return byteArrayInputStream.read();
                }
            };
        }
    }
}