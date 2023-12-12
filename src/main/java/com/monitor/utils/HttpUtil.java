package com.monitor.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
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
        return response.statusCode() == 200 ? response.body() : null;
    }

    public static String doGet(String url, Map<String, String> properties) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            return execute(connection, "get", properties, null, 0, 0, true, true);
        } catch (IOException e) {
            log.error(String.format("Request %s failed, ", url), e);
        }

        return null;
    }

    public static String doPost(String url, Map<String, String> properties, String payload) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            return execute(connection, "post", properties, payload, 0, 0, true, true);
        } catch (IOException e) {
            log.error(String.format("Request %s failed, ", url), e);
        }

        return null;
    }

    public static String execute(HttpURLConnection connection, String method, Map<String, String> properties, String payload, int connectTimeout, int readTimeout, boolean doOutput, boolean doInput) throws IOException {
        connection.setConnectTimeout(connectTimeout <= 0 ? 15000 : connectTimeout);
        connection.setReadTimeout(readTimeout <= 0 ? 60000 : readTimeout);

        if (!CollectionUtils.isEmpty(properties)) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        if ("POST".equals(method)) {
            connection.setDoOutput(doOutput);
            connection.setDoInput(doInput);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(payload.getBytes());
            }
        } else {
            connection.connect();
        }

        if (connection.getResponseCode() == 200) {
            try (InputStream inputStream = connection.getInputStream(); BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));) {
                StringBuilder sbf = new StringBuilder();
                String temp;
                while ((temp = bufferedReader.readLine()) != null) {
                    sbf.append(temp);
                    sbf.append("\r\n");
                }
                return sbf.toString();
            } finally {
                Objects.requireNonNull(connection).disconnect();
            }
        }
        return null;
    }

    public static byte[] download(String url, int maxRetry) {
        byte[] downloadContents = new byte[0];
        do {
            try {
                downloadContents = download(url);
                break;
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

        log.error("Download url " + url + "failed");
        return downloadContents;
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
