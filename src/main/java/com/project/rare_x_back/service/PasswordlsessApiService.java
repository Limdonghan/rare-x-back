package com.project.rare_x_back.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.rare_x_back.dto.response.PasswordlessResponseDto;
import com.project.rare_x_back.dto.response.PasswordlessResultResponseDto;
import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.UserRepository;
import com.project.rare_x_back.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class PasswordlsessApiService {

    private final RestClient restClient;
    @Value("${serving.api.url}")
    private String servingUrl;

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";


    /**
     * 사용자 등록 여부 확인 API 호출
     */
    public String checkUserStatus(String email) {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        /// URL 생성
        URI uri = UriComponentsBuilder.fromUriString(servingUrl) /// http://54.180.../api/passwordless
                .path("/status")                               /// + /status
                .queryParam("userId", email)                   /// + ?userId=...
                .build()
                .toUri();

        /// 실제 요청 보내기
        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);

    }
    /**
     * 사용자 등록 API 호출
     */
    public String registerUser(String email){
        userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        URI uri = UriComponentsBuilder.fromUriString(servingUrl) /// http://54.180.../api/passwordless
                .path("/register")
                .queryParam("userId", email)
                .build()
                .toUri();

        return restClient.post()
                .uri(uri)
                .retrieve()
                .body(String.class);
    }

    /**
     * 로그인 인증 요청
     */
    public String triggerLogin(String email,String ip){
        userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        URI uri = UriComponentsBuilder.fromUriString(servingUrl) /// http://54.180.../api/passwordless
                .path("/login-trigger")
                .queryParam("userId", email)
                .queryParam("ip", ip)
                .build()
                .toUri();

        return restClient.post()
                .uri(uri)
                .retrieve()
                .body(String.class);
    }

    /**
     * 인증 결과 확인
     */
    public PasswordlessResponseDto checkResult(String email, String sessionId) throws JsonProcessingException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        URI uri = UriComponentsBuilder.fromUriString(servingUrl) /// http://54.180.../api/passwordless
                .path("/result")
                .queryParam("userId", email)
                .queryParam("sessionId", sessionId)
                .build()
                .toUri();

        String result = restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(result);

        // 1단계: 최상위 응답에서 "data" 필드 추출
        JsonNode dataFieldNode = rootNode.get("data");

        // 2단계: "data" 필드가 문자열이면 다시 파싱
        JsonNode actualDataNode;
        if (dataFieldNode.isTextual()) {
            String dataString = dataFieldNode.asText();
            actualDataNode = mapper.readTree(dataString);
        } else {
            actualDataNode = dataFieldNode;
        }

        PasswordlessResultResponseDto build = PasswordlessResultResponseDto.builder()
                .auth(actualDataNode.get("auth").asText())
                .userId(actualDataNode.get("userId").asText())
                .hash(actualDataNode.get("hash").asText())
                .build();

        if(build.getAuth().equals("Y")){
            String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getRole().name(), user.getEmail());
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId(), user.getRole().name());

            String key = REFRESH_TOKEN_PREFIX + user.getUserId();
            redisTemplate.opsForValue().set(key, refreshToken, 7, TimeUnit.DAYS);

            return PasswordlessResponseDto.builder()
                    .result("OK")
                    .data(build)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .name(user.getName())
                    .role(user.getRole().name())
                    .build();
        }

        return PasswordlessResponseDto.builder()
                .result("ERROR")
                .data(build)
                .accessToken(null)
                .refreshToken(null)
                .name(null)
                .role(null)
                .build();

    }
    /**
     * 인증 취소
     */
    public String cancel (String email, String sessionId){
        URI uri = UriComponentsBuilder.fromUriString(servingUrl) /// http://54.180.../api/passwordless
                .path("/cancel")
                .queryParam("userId", email)
                .queryParam("sessionId", sessionId)
                .build()
                .toUri();

        return restClient.post()
                .uri(uri)
                .retrieve()
                .body(String.class);
    }
    /**
     * 사용자 탈퇴
     */
    @Transactional
    public PasswordlessResponseDto withdrawalAp(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        URI uri = UriComponentsBuilder.fromUriString(servingUrl) /// http://54.180.../api/passwordless
                .path("/withdrawal")
                .queryParam("userId", email)
                .build()
                .toUri();
                

        String result = restClient.post()
                .uri(uri)
                .retrieve()
                .body(String.class);

        return PasswordlessResponseDto.builder()
                .result("OK")
                .data(result)
                .message("패스워드리스 해지가 완료 되었습니다.")
                .build();
    }


}
