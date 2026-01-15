package com.project.rare_x_back.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.rare_x_back.enums.PasswordlessApiEndpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordlessApiClientService {

    @Value("${passwordless.server-key}")
    private String serverKey;

    @Value("${passwordless.rest-check-url}")
    private String restCheckUrl;

    private final ObjectMapper objectMapper;
    private final RestClient restClient;


    public String callApi(PasswordlessApiEndpoint passwordlessApiEndpoint, Map<String, String> params) {
        try {
            return restClient.post()
                    .uri(uriBuilder -> {
                        uriBuilder.scheme("http")
                                .host(restCheckUrl.replaceFirst("^https?://", "").split(":")[0])
                                .port(restCheckUrl.contains(":") ?
                                        Integer.parseInt(restCheckUrl.replaceFirst("^https?://", "").split(":")[1]) : 80)
                                .path(passwordlessApiEndpoint.getPath());
                        if (params != null) {
                            params.forEach(uriBuilder::queryParam);
                        }
                        return uriBuilder.build();
                    })
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .retrieve()
                    .body(String.class);

        } catch (Exception e) {
            log.error("Passwordless API call failed: {}", e.getMessage(), e);
            return null;
        }
    }

    public String getOneTimeToken(String userId) {
        Map<String, String> params = Map.of("userId", userId);
        String response = callApi(PasswordlessApiEndpoint.GET_TOKEN_FOR_ONE_TIME, params);

        if (response != null) {
            try {
                JsonNode jsonResponse = objectMapper.readTree(response);
                JsonNode data = jsonResponse.get("data");
                String encryptedToken = data.get("token").asText();
                return decryptAES(encryptedToken);
            } catch (Exception e) {
                log.error("Failed to get one-time token", e);
            }
        }
        return null;
    }

    public String decryptAES(String encrypted) {
        try {
            byte[] key = serverKey.getBytes();
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(key));
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            return new String(decryptedBytes, "UTF-8");
        } catch (Exception e) {
            log.error("AES decryption failed", e);
            return null;
        }
    }



}
