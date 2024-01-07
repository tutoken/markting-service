package com.monitor.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.monitor.utils.TimeUtil.calTimeElapse;

@Slf4j
public class HttpUtil {
    public static String get(String url) {
        return calTimeElapse(String.format("get %s", url), () -> request(url, null, null, 0, 0));
    }

    public static String get(String url, int maxRetry) {
        return calTimeElapse(String.format("get %s", url), () -> request(url, null, null, 0, maxRetry));
    }

    public static String get(String url, Map<String, String> properties, int maxRetry) {
        return calTimeElapse(String.format("get %s", url), () -> request(url, properties, null, 0, maxRetry));
    }

    public static String get(String url, Map<String, String> properties) {
        return calTimeElapse(String.format("get %s", url), () -> request(url, properties, null, 0, 0));
    }

    public static String post(String url, String payload) {
        return calTimeElapse(String.format("post %s payload is %s", url, payload), () -> request(url, null, payload, 0, 0));
    }

    public static String post(String url, String payload, int maxRetry) {
        return calTimeElapse(String.format("post %s payload is %s", url, payload), () -> request(url, null, payload, 0, maxRetry));
    }

    public static String request(String url, Map<String, String> properties, String payload, long duration, int maxRetry) throws InterruptedException {
//        log.info(String.format("Execute request url is: %s", url));

        Builder builder = HttpRequest.newBuilder().uri(URI.create(url));

        if (!CollectionUtils.isEmpty(properties)) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }
        if (payload != null) {
            builder.POST(HttpRequest.BodyPublishers.ofString(payload));
        }
        builder.timeout(Duration.ofMinutes(duration <= 0 ? 1L : duration));

        do {
            try {
                return execute(builder.build());
            } catch (Exception e) {
                maxRetry = maxRetry - 1;
                if (maxRetry >= 0) {
                    TimeUnit.SECONDS.sleep(3);
                    log.error(String.format("Request %s error, retry %d left, cause: ", url, maxRetry), e.getMessage());
                }
            }
        } while (maxRetry >= 0);

        log.error(String.format("Request %s failed,", url));

        return null;
    }

    private static String execute(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        log.debug("Response is " + response);
        return response.statusCode() == 200 ? response.body() : null;
    }

    public static byte[] download(String url, int maxRetry) {
        do {
            try {
                return download(url);
            } catch (Exception e) {
                maxRetry = maxRetry - 1;
                if (maxRetry >= 0) {
                    try {
                        TimeUnit.SECONDS.sleep(3);
                    } catch (InterruptedException ex) {
                        log.error(String.format("Download %s error %d retry left, cause: ", url, maxRetry), e);
                    }
                    log.error(String.format("Download %s error %d retry left, cause: ", url, maxRetry), e);
                }
            }
        } while (maxRetry >= 0);

        log.error("Download url " + url + " failed");
        return new byte[0];
    }

    private static byte[] download(String urlString) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36");
        connection.setRequestMethod("GET");
        connection.connect();

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            InputStream inputStream = connection.getInputStream();
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            return output.toByteArray();

        } else {
            throw new IOException("GET request failed with response code: " + responseCode);
        }
    }
}
