package com.project.rare_x_back.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.rare_x_back.dto.request.PasswordlessRequestDto;
import com.project.rare_x_back.dto.response.PasswordlessResponseDto;
import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.enums.PasswordlessApiEndpoint;
import com.project.rare_x_back.exceptions.AuthException;
import com.project.rare_x_back.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordlessService {

    @Value("${passwordless.push-connector-url}")
    private String pushConnectorUrl;

    private final PasswordlessApiClientService passwordlessApiClient;
    private final UserRepository userRepository;
    private final HttpSession httpSession;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;

    private static final int TOKEN_EXPIRY_MS = 5 * 60 * 1000; // 5 minutes
    public static final String LOGIN_USER_EMAIL = "LOGIN_USER_EMAIL";


    @Transactional(readOnly = true)
    public PasswordlessResponseDto verifyManagementAccess(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("Invalid ID or password"));

        if (!passwordEncoder.matches(password,user.getPassword())) {
            throw new AuthException("Invalid ID or password");
        }

        String token = UUID.randomUUID().toString();
        long currentTime = System.currentTimeMillis();

        httpSession.setAttribute("passwordlessToken", token);
        httpSession.setAttribute("passwordlessTime", currentTime);

        return PasswordlessResponseDto.builder()
                .result("OK")
                .data(token)
                .build();
    }


    //패스워드리스 서비스 등록 여부 확인
    @Transactional(readOnly = true)
    public PasswordlessResponseDto checkRegistrationStatus(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new AuthException("ID [" + email + "] does not exist");
        }

        Map<String, String> params = Map.of("userId", email);
        String response = passwordlessApiClient.callApi(PasswordlessApiEndpoint.IS_AP, params);

        Object parsedData = parseJsonString(response);

        return PasswordlessResponseDto.builder()
                .result("OK")
                .data(parsedData)
                .build();
    }

    //패스워드리스 서비스 등록 (요청)
    @Transactional
    public PasswordlessResponseDto registerPasswordless(PasswordlessRequestDto request, HttpServletRequest httpRequest) {
        validateToken(request.getToken());

        User user = userRepository.findByEmail(request.getUserId())
                .orElseThrow(() -> new AuthException("ID [" + request.getUserId() + "] does not exist"));

        if (user.getPasswordlessEnabled()) {
            throw new AuthException("이미 패스워드리스 서비스를 사용 중입니다");
        }

        Map<String, String> params = new HashMap<>();
        params.put("userId", request.getUserId());

        String response = passwordlessApiClient.callApi(PasswordlessApiEndpoint.JOIN_AP, params);

        userRepository.updatePasswordlessStatus(
                user.getEmail(),
                true);

        log.info("패스워드리스 등록 완료: {}", user.getEmail());

        Object parsedData = parseJsonString(response);

        return PasswordlessResponseDto.builder()
                .result("OK")
                .data(parsedData)
                .pushConnectUrl(pushConnectorUrl)
                .build();
    }


    //패스워드리스 서비스 해지 (요청)
    @Transactional
    public PasswordlessResponseDto withdrawPasswordless(PasswordlessRequestDto request) {
        validateToken(request.getToken());

        User user = userRepository.findByEmail(request.getUserId())
                .orElseThrow(() -> new AuthException("ID [" + request.getUserId() + "] does not exist"));

        if (!user.getPasswordlessEnabled()) {
            throw new AuthException("패스워드리스 서비스를 사용하고 있지 않습니다");
        }

        Map<String, String> params = Map.of("userId", request.getUserId());
        String response = passwordlessApiClient.callApi(PasswordlessApiEndpoint.WITHDRAWAL_AP, params);

        Object parsedData = parseJsonString(response);

        return PasswordlessResponseDto.builder()
                .result("OK")
                .data(parsedData)
                .build();
    }


    //일회용 토큰 발급
    @Transactional(readOnly = true)
    public PasswordlessResponseDto getOneTimeToken(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new AuthException("ID [" + email + "] does not exist");
        }

        String oneTimeToken = passwordlessApiClient.getOneTimeToken(email);

        return PasswordlessResponseDto.builder()
                .result("OK")
                .oneTimeToken(oneTimeToken)
                .build();
    }

    //로그인 시도 (푸시알림전송)
    @Transactional
    public PasswordlessResponseDto requestAuthentication(String email, String token, HttpServletRequest httpRequest) {
        if (!userRepository.existsByEmail(email)) {
            throw new AuthException("ID [" + email + "] does not exist");
        }

        String sessionId = System.currentTimeMillis() + "_sessionId";
        String random = UUID.randomUUID().toString();
        String clientIp = getClientIp(httpRequest);

        Map<String, String> params = new HashMap<>();
        params.put("userId", email);
        params.put("token", token);
        params.put("clientIp", clientIp);
        params.put("sessionId", sessionId);
        params.put("random", random);
        params.put("password", "");

        String response = passwordlessApiClient.callApi(PasswordlessApiEndpoint.GET_SP, params);

        Object parsedData = parseJsonString(response);

        return PasswordlessResponseDto.builder()
                .result("OK")
                .data(parsedData)
                .sessionId(sessionId)
                .build();
    }

    //사용자가 폰에서 승인했는지 확인
    @Transactional
    public PasswordlessResponseDto checkAuthenticationResult(String email, String sessionId) {
        if (!userRepository.existsByEmail(email)) {
            throw new AuthException("ID [" + email + "] does not exist");
        }

        Map<String, String> params = new HashMap<>();
        params.put("userId", email);
        params.put("sessionId", sessionId);

        String response = passwordlessApiClient.callApi(PasswordlessApiEndpoint.RESULT, params);

        try {
            JsonNode jsonResponse = objectMapper.readTree(response);
            JsonNode data = jsonResponse.get("data");

            if (data != null) {
                String auth = data.get("auth").asText();
                if ("Y".equals(auth)) {
                    String newPassword = System.currentTimeMillis() + ":" + email;
                    String encodedPassword = passwordEncoder.encode(newPassword);
                    userRepository.updatePasswordByEmail(email, encodedPassword);
                    httpSession.setAttribute("LOGIN_USER_EMAIL", email);
                    log.info("Passwordless authentication successful for user: {}", email);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse authentication result", e);
        }

        Object parsedData = parseJsonString(response);

        return PasswordlessResponseDto.builder()
                .result("OK")
                .data(parsedData)
                .build();
    }

    //로그인 시도 취소
    @Transactional(readOnly = true)
    public PasswordlessResponseDto cancelAuthentication(String email, String sessionId) {
        if (!userRepository.existsByEmail(email)) {
            throw new AuthException("ID [" + email + "] does not exist");
        }

        Map<String, String> params = new HashMap<>();
        params.put("userId", email);
        params.put("sessionId", sessionId);

        String response = passwordlessApiClient.callApi(PasswordlessApiEndpoint.CANCEL, params);
        Object parsedData = parseJsonString(response);
        return PasswordlessResponseDto.builder()
                .result("OK")
                .data(parsedData)
                .build();
    }

    //로그인 후 발급한 “임시 인증 토큰”이 진짜이고, 아직 유효한지 검증
    private void validateToken(String token) {
        String sessionToken = (String) httpSession.getAttribute("passwordlessToken");
        Long sessionTime = (Long) httpSession.getAttribute("passwordlessTime");

        if (sessionToken == null || !sessionToken.equals(token)) {
            throw new AuthException("Invalid token");
        }

        if (sessionTime == null || System.currentTimeMillis() - sessionTime > TOKEN_EXPIRY_MS) {
            throw new AuthException("Token expired");
        }
    }

    //로그인 시도한 사용자의 실제 IP 주소 추출 -> 이상 로그인 탐지
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }

    /**
     * JSON 문자열을 객체로 변환
     * @param jsonString JSON 문자열
     * @return 파싱된 객체 (Map 또는 String)
     */
    private Object parseJsonString(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return null;
        }

        try {
            // JSON 문자열인지 확인 ('{' 또는 '[' 로 시작)
            String trimmed = jsonString.trim();
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                // JSON 파싱
                return objectMapper.readValue(jsonString, Object.class);
            } else {
                // 일반 문자열
                return jsonString;
            }
        } catch (Exception e) {
            log.warn("JSON 파싱 실패, 원본 문자열 반환: {}", e.getMessage());
            return jsonString;
        }
    }

}
