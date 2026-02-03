package com.project.rare_x_back.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.rare_x_back.dto.request.PasswordlessRequestDto;
import com.project.rare_x_back.dto.request.PasswordlessWithdrawRequestDto;
import com.project.rare_x_back.dto.response.PasswordlessResponseDto;
import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.enums.PasswordlessApiEndpoint;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.UserRepository;
import com.project.rare_x_back.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordlessService {

    private final EmailService emailService;
    @Value("${passwordless.push-connector-url}")
    private String pushConnectorUrl;

    private final PasswordlessApiClientService passwordlessApiClient;
    private final UserRepository userRepository;
    private final HttpSession httpSession;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    private static final int TOKEN_EXPIRY_MS = 5 * 60 * 1000; // 5 minutes
    public static final String LOGIN_USER_EMAIL = "LOGIN_USER_EMAIL";
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    //등록 위한 본인 확인, 일회용 토큰 발급
    @Transactional(readOnly = true)
    public PasswordlessResponseDto verifyManagementAccess(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        String token = UUID.randomUUID().toString();
        long currentTime = System.currentTimeMillis();

        httpSession.setAttribute("passwordlessToken", token);
        httpSession.setAttribute("passwordlessTime", currentTime);

        log.info("일회용 토큰 발급:{}", token);

        return PasswordlessResponseDto.builder()
                .result("OK")
                .data(token)
                .build();
    }


    //패스워드리스 서비스 등록 여부 확인
    @Transactional(readOnly = true)
    public PasswordlessResponseDto checkRegistrationStatus(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        Map<String, String> params = Map.of("userId", email);
        String response = passwordlessApiClient.callApi(PasswordlessApiEndpoint.IS_AP, params);

        Object parsedData = parseJsonString(response);
        log.info("1. 패스워드 등록 여부 확인: {}" , parsedData);
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
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getPasswordlessEnabled()) {
            throw new CustomException(ErrorCode.PASSWORDLESS_ALREADY_IN_USE);
        }

        Map<String, String> params = new HashMap<>();
        params.put("userId", request.getUserId());

        String response = passwordlessApiClient.callApi(PasswordlessApiEndpoint.JOIN_AP, params);

        Object parsedData = parseJsonString(response);

        userRepository.updatePasswordlessStatus(user.getEmail(), true);

        log.info("2. 패스워드리스 등록 완료: {}", user.getEmail());

        return PasswordlessResponseDto.builder()
                .result("OK")
                .data(parsedData)
                .pushConnectUrl(pushConnectorUrl)
                .build();
    }


    //패스워드리스 서비스 해지 (요청)
    @Transactional
    public PasswordlessResponseDto withdrawPasswordless(PasswordlessWithdrawRequestDto request) {
        User user = userRepository.findByEmail(request.getUserId()).orElseThrow(() ->
                new CustomException(ErrorCode.USER_NOT_FOUND, request.getUserId() + "는 존재하지 않는 사용자입니다."));

        if (!user.getPasswordlessEnabled()) {
            throw new CustomException(ErrorCode.PASSWORDLESS_NOT_REGISTERED);
        }

        Map<String, String> params = Map.of("userId", request.getUserId());
        String response = passwordlessApiClient.callApi(
                PasswordlessApiEndpoint.WITHDRAWAL_AP, params);

        //임시 비번 생성 및 메일발송 (레디스 저장)
        String tempPassword = emailService.sendTempPassword(user.getEmail());

        //DB에 임시 패스워드 저장(암호화)
        String encodedTempPassword = passwordEncoder.encode(tempPassword);
        user.updatePassword(encodedTempPassword);

        //패스워드리스 비활성화
        userRepository.updatePasswordlessStatus(user.getEmail(), false);

        log.info("패스워드리스 해지 완료 : email={}", user.getEmail());

        Object parsedData = parseJsonString(response);

        return PasswordlessResponseDto.builder()
                .result("OK")
                .data(parsedData)
                .message("패스워드리스 해지가 완료 되었습니다. 이메일로 발송된 임시 비밀번호로 로그인해주세요.")
                .build();
    }


    //일회용 토큰 발급
    @Transactional(readOnly = true)
    public PasswordlessResponseDto getOneTimeToken(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
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
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
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

    //사용자가 폰에서 승인했는지 확인 (Long Polling 60s)
    @Transactional
    public PasswordlessResponseDto checkAuthenticationResult(String email, String sessionId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Map<String, String> params = new HashMap<>();
        params.put("userId", email);
        params.put("sessionId", sessionId);

        String response = null;
        Object parsedData = null;
        long startTime = System.currentTimeMillis();
        long maxDuration = 60000; // 60 seconds

        while (System.currentTimeMillis() - startTime < maxDuration) {
            try {
                response = passwordlessApiClient.callApi(PasswordlessApiEndpoint.RESULT, params);
                JsonNode jsonResponse = objectMapper.readTree(response);
                JsonNode data = jsonResponse.get("data");

                if (data != null) {
                    String auth = data.get("auth").asText();
                    if ("Y".equals(auth)) {
                        String newPassword = UUID.randomUUID().toString();
                        String encodedPassword = passwordEncoder.encode(newPassword);
                        userRepository.updatePasswordByEmail(email, encodedPassword);
                        httpSession.setAttribute("LOGIN_USER_EMAIL", email);
                        log.info("Passwordless authentication successful for user: {}", email);

                        // 토큰 생성
                        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(),user.getRole().name(),user.getEmail());
                        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId(),user.getRole().name());

                        // Refresh Token을 Redis에 저장 (7일)
                        String key = REFRESH_TOKEN_PREFIX + user.getUserId();
                        redisTemplate.opsForValue().set(key, refreshToken, 7, TimeUnit.DAYS);

                        parsedData = parseJsonString(response);
                        return PasswordlessResponseDto.builder()
                                .result("OK")
                                .data(parsedData)
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .name(user.getName())
                                .role(user.getRole().name())
                                .build();
                    }
                }

                // Wait for 2 seconds before retrying
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Polling interrupted", e);
                break;
            } catch (Exception e) {
                log.error("Failed to check authentication result", e);
                // Continue polling even if one request fails? Or break?
                // Let's sleep and retry mostly
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        parsedData = (response != null) ? parseJsonString(response) : null;
        return PasswordlessResponseDto.builder()
                .result("TIMEOUT")
                .data(parsedData)
                .build();
    }


    //로그인 시도 취소
    @Transactional(readOnly = true)
    public PasswordlessResponseDto cancelAuthentication(String email, String sessionId) {
        if (!userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
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
            throw new CustomException(ErrorCode.TEMPORARY_TOKEN_NOT_FOUND);
        }

        if (sessionTime == null || System.currentTimeMillis() - sessionTime > TOKEN_EXPIRY_MS) {
            throw new CustomException(ErrorCode.TEMPORARY_TOKEN_EXPIRED);
        }
        log.info("토큰 유효 시간:{}",sessionTime);
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
