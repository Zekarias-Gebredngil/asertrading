package com.aser.asertrading.common.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
public class S3StorageService {

    @Value("${app.storage.s3.enabled:false}")
    private boolean s3Enabled;

    @Value("${app.storage.s3.bucket:${AWS_S3_BUCKET:}}")
    private String s3Bucket;

    @Value("${app.storage.s3.region:${AWS_REGION:us-east-1}}")
    private String s3Region;

    @Value("${app.storage.s3.access-key:${AWS_ACCESS_KEY_ID:}}")
    private String s3AccessKey;

    @Value("${app.storage.s3.secret-key:${AWS_SECRET_ACCESS_KEY:}}")
    private String s3SecretKey;

    @Value("${app.storage.s3.endpoint:${AWS_S3_ENDPOINT:}}")
    private String s3Endpoint;

    @Value("${app.storage.s3.public-url-prefix:${AWS_S3_PUBLIC_URL_PREFIX:}}")
    private String s3PublicUrlPrefix;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.media.base-url:http://localhost:8085}")
    private String mediaBaseUrl;

    private static final DateTimeFormatter AMZ_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter DATE_STAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public boolean isS3Configured() {
        return (s3Enabled || (s3Bucket != null && !s3Bucket.isBlank()))
                && s3AccessKey != null && !s3AccessKey.isBlank()
                && s3SecretKey != null && !s3SecretKey.isBlank();
    }

    /**
     * Store image either on AWS S3 / Cloud Object Storage or local persistent directory
     * and return the globally accessible public URL.
     */
    public StorageResult store(String folder, String fileName, InputStream inputStream, byte[] fileBytes, String contentType) {
        String safeFolder = (folder == null || folder.isBlank()) ? "images" : folder.replaceAll("[^a-zA-Z0-9_-]", "");
        String s3Key = safeFolder + "/" + fileName;

        // 1. Try S3 Cloud Storage if configured
        if (isS3Configured()) {
            try {
                String s3Url = uploadToS3(s3Key, fileBytes, contentType);
                log.info("[S3StorageService] Successfully uploaded to AWS S3: {}", s3Url);
                return new StorageResult(s3Url, s3Url, fileName, "S3");
            } catch (Exception ex) {
                log.error("[S3StorageService] AWS S3 upload failed, falling back to persistent server storage: {}", ex.getMessage(), ex);
            }
        } else {
            log.info("[S3StorageService] S3 not fully configured (bucket='{}', accessKeySet={}). Using persistent server storage.",
                    s3Bucket, (s3AccessKey != null && !s3AccessKey.isBlank()));
        }

        // 2. Persistent Server Storage fallback
        try {
            Path targetDir = Paths.get(uploadDir, safeFolder).toAbsolutePath().normalize();
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            Path destination = targetDir.resolve(fileName);
            Files.write(destination, fileBytes);

            String relativeUrl = "/uploads/" + safeFolder + "/" + fileName;
            String baseUrl = mediaBaseUrl.endsWith("/") ? mediaBaseUrl.substring(0, mediaBaseUrl.length() - 1) : mediaBaseUrl;
            String absoluteUrl = baseUrl + relativeUrl;

            log.info("[S3StorageService] File permanently stored at server path: {}", destination);
            return new StorageResult(absoluteUrl, relativeUrl, fileName, "LOCAL_PERSISTENT");
        } catch (IOException e) {
            log.error("[S3StorageService] Failed to persist file locally", e);
            throw new RuntimeException("Storage failure: " + e.getMessage(), e);
        }
    }

    /**
     * Pure Java AWS SigV4 S3 PUT Object implementation
     */
    private String uploadToS3(String key, byte[] content, String contentType) throws Exception {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String amzDate = now.format(AMZ_DATE_FORMAT);
        String dateStamp = now.format(DATE_STAMP_FORMAT);

        String host;
        String requestUri;
        String targetUrl;

        if (s3Endpoint != null && !s3Endpoint.isBlank()) {
            URI endpointUri = URI.create(s3Endpoint);
            host = endpointUri.getHost();
            requestUri = "/" + s3Bucket + "/" + key;
            targetUrl = s3Endpoint + requestUri;
        } else {
            host = s3Bucket + ".s3." + s3Region + ".amazonaws.com";
            requestUri = "/" + key;
            targetUrl = "https://" + host + requestUri;
        }

        String payloadHash = hex(sha256(content));
        String canonicalHeaders = "host:" + host + "\n"
                + "x-amz-content-sha256:" + payloadHash + "\n"
                + "x-amz-date:" + amzDate + "\n";
        String signedHeaders = "host;x-amz-content-sha256;x-amz-date";

        String canonicalRequest = "PUT\n"
                + requestUri + "\n"
                + "\n" // canonical query string
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + payloadHash;

        String algorithm = "AWS4-HMAC-SHA256";
        String credentialScope = dateStamp + "/" + s3Region + "/s3/aws4_request";
        String stringToSign = algorithm + "\n"
                + amzDate + "\n"
                + credentialScope + "\n"
                + hex(sha256(canonicalRequest.getBytes(StandardCharsets.UTF_8)));

        byte[] signingKey = getSignatureKey(s3SecretKey, dateStamp, s3Region, "s3");
        String signature = hex(hmacSha256(signingKey, stringToSign));

        String authHeader = algorithm + " "
                + "Credential=" + s3AccessKey + "/" + credentialScope + ", "
                + "SignedHeaders=" + signedHeaders + ", "
                + "Signature=" + signature;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Host", host)
                .header("x-amz-date", amzDate)
                .header("x-amz-content-sha256", payloadHash)
                .header("Authorization", authHeader)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(content));

        if (contentType != null && !contentType.isBlank()) {
            requestBuilder.header("Content-Type", contentType);
        }

        HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        int statusCode = response.statusCode();
        if (statusCode >= 200 && statusCode < 300) {
            if (s3PublicUrlPrefix != null && !s3PublicUrlPrefix.isBlank()) {
                String prefix = s3PublicUrlPrefix.endsWith("/")
                        ? s3PublicUrlPrefix.substring(0, s3PublicUrlPrefix.length() - 1)
                        : s3PublicUrlPrefix;
                return prefix + "/" + key;
            }
            return targetUrl;
        } else {
            throw new RuntimeException("S3 HTTP " + statusCode + " error: " + response.body());
        }
    }

    private static byte[] sha256(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(data);
    }

    private static byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) throws Exception {
        byte[] kSecret = ("AWS4" + key).getBytes(StandardCharsets.UTF_8);
        byte[] kDate = hmacSha256(kSecret, dateStamp);
        byte[] kRegion = hmacSha256(kDate, regionName);
        byte[] kService = hmacSha256(kRegion, serviceName);
        return hmacSha256(kService, "aws4_request");
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    public record StorageResult(String url, String relativeUrl, String fileName, String storageType) {}
}

