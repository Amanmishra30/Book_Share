package com.bookshare.service;

import com.bookshare.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;

/** Signed upload straight to Cloudinary's REST API (free tier works). Enable with STORAGE_TYPE=cloudinary. */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "cloudinary")
public class CloudinaryStorageService implements StorageService {
    private final String cloud;
    private final String apiKey;
    private final String apiSecret;
    private final RestTemplate rest = new RestTemplate();

    public CloudinaryStorageService(@Value("${app.cloudinary.cloud-name}") String cloud,
                                    @Value("${app.cloudinary.api-key}") String apiKey,
                                    @Value("${app.cloudinary.api-secret}") String apiSecret) {
        this.cloud = cloud;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String store(MultipartFile file) {
        StorageService.requireImage(file);
        try {
            long ts = Instant.now().getEpochSecond();
            String signature = sha1("folder=bookshare&timestamp=" + ts + apiSecret);
            String filename = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override public String getFilename() { return filename; }
            });
            body.add("api_key", apiKey);
            body.add("timestamp", String.valueOf(ts));
            body.add("folder", "bookshare");
            body.add("signature", signature);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            ResponseEntity<Map> res = rest.postForEntity("https://api.cloudinary.com/v1_1/" + cloud + "/image/upload",
                    new HttpEntity<>(body, headers), Map.class);
            Object url = res.getBody() == null ? null : res.getBody().get("secure_url");
            if (url == null) throw new IllegalStateException("No secure_url in Cloudinary response");
            return url.toString();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Photo upload to Cloudinary failed.");
        }
    }

    private static String sha1(String s) throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : d) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
