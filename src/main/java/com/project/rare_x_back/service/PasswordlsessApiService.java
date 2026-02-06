package com.project.rare_x_back.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.dto.response.PasswordlessResponseDto;
import com.project.rare_x_back.dto.response.PasswordlessResultResponseDto;
import com.project.rare_x_back.dto.response.PasswordlessStatusResponseDto;
import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.UserRepository;
import com.project.rare_x_back.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
@Slf4j
@RequiredArgsConstructor
public class PasswordlsessApiService {

    private final RestClient restClient;
    @Value("${serving.api.url}")
    private String servingUrl;

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";


    /**
     * 사용자 등록 여부 확인 API 호출
     */
    public Boolean checkUserStatus(String email) {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        /// URL 생성
        URI uri = UriComponentsBuilder.fromUriString(servingUrl)
                .path("/status")
                .queryParam("userId", email)
                .build()
                .toUri();

        /// 응답 요청
        ApiResponse<PasswordlessStatusResponseDto> body = restClient.get()
                .uri(uri)
                .retrieve()
                .body(PasswordlessStatusResponseDto.responseType);

            return body.getData().isExist();

    }

    /**
     * 로컬 사용자 패스워드리스 활성화 여부 확인
     */
    public Boolean checkLocalUserStatus(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return user.getPasswordlessEnabled();
    }

    /**
     * 사용자 등록 API 호출
     */
    public String registerUser(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Boolean userStatus = checkUserStatus(email);

        if (!userStatus && !user.getPasswordlessEnabled()) {
        URI uri = UriComponentsBuilder.fromUriString(servingUrl)
                .path("/register")
                .queryParam("userId", email)
                .build()
                .toUri();

            return restClient.post()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

        }else {
            throw new RuntimeException("사용자 등록 API 호출을 실패했습니다. 패스워드리스 등록되지 않은 사용자입니다.");
        }

    }
    /**
     * 패스워드리스 활성화
     */
    @Transactional
    public void passwordlessEnabled(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        userRepository.updatePasswordlessStatus(user.getEmail(),true);
    }

    /**
     * 로그인 인증 요청
     */
    public String triggerLogin(String email,String ip) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Boolean userStatus = checkUserStatus(email);
        if (userStatus && user.getPasswordlessEnabled()) {
        URI uri = UriComponentsBuilder.fromUriString(servingUrl)
                .path("/login-trigger")
                .queryParam("userId", email)
                .queryParam("ip", ip)
                .build()
                .toUri();

        return restClient.post()
                .uri(uri)
                .retrieve()
                .body(String.class);
        }else {
            throw new RuntimeException("로그인 인증 요청이 실패했습니다.");
        }
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

        /// 1단계: 최상위 응답에서 "data" 필드 추출
        JsonNode dataFieldNode = rootNode.get("data");

        /// 2단계: "data" 필드가 문자열이면 다시 파싱
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
                .result("WAIT")
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

        userRepository.updatePasswordlessStatus(user.getEmail(),false);

        return PasswordlessResponseDto.builder()
                .result("OK")
                .data(result)
                .message("패스워드리스 해지가 완료 되었습니다.")
                .build();
    }


}
