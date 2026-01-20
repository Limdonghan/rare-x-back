package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.request.AddressRegisterRequestDto;
import com.project.rare_x_back.dto.response.JusoResponseDto;
import com.project.rare_x_back.dto.response.UserAddressResponseDto;
import com.project.rare_x_back.entity.Address;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.AddressRepository;
import com.project.rare_x_back.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressService {

    private final WebClient jusoWebClient;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    @Value("${JUSO_CONFIRM_KEY}")
    private String apiKey;

    // 주소 검색
    public JusoResponseDto searchAddress(String keyword, int currentPage) {
        log.info("도로명 API key = [{}]", apiKey);
        return jusoWebClient.get()
                .uri(
                        uriBuilder -> uriBuilder
                                .path("/addrlink/addrLinkApi.do")   // base URL 뒤에 붙는 리소스 경로
                                .queryParam("confmKey", apiKey) // 인증키
                                .queryParam("keyword", keyword) // 검색어
                                .queryParam("currentPage", currentPage) // 페이지 번호
                                .queryParam("firstSort")
                                .queryParam("countPerPage", 10) // 한 페이지 당 10개
                                .queryParam("resultType", "json") // 응답 형식
                                .build()) // 최종 요청 url 생성
                .retrieve() // HTTP 요청 전송 및 응답 처리 시작
                .bodyToMono(JusoResponseDto.class)  // Json을 -> Dto로 변환
                .block();   // 주소 검색 결과가 필요하므로 결과가 올 때 까지 대기. (동기 처리)
    }

    //주소 등록
    @Transactional
    public void createAddress (Long userId, AddressRegisterRequestDto requestDto) {

        // 유저의 현재 주소 갯수 조회
        int addressCount = addressRepository.countByUser_UserId(userId);

        // 주소 갯수가 0이면 -> 정책에 맞게 최초 등록 주소는 기본배송지로 설정
        // 주소가 이미 있는데 이번에 등록하는 주소를 기본으로 설정 -> 기존 기본 배송 해제
        boolean isDefault = requestDto.isDefaultAddress();
        if (addressCount == 0) {
            isDefault = true;
        } else if (isDefault) {
            addressRepository.updateAllIsDefaultToFalse(userId);
        }
        // 한 유저당 등록 가능 배송지는 최대 10개 정책
        if (addressCount >= 10) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "배송지는 최대 10개까지 등록 가능 합니다.");
        }

        // 엔티티 생성 및 저장
        Address address = Address.builder()
                .user(userRepository.getReferenceById(userId))
                .recipientName(requestDto.getRecipientName())   // 유저가 직접 쓴 받는이
                .postalCode(requestDto.getPostalCode())         // 프론트가 보내준 우편번호
                .address(requestDto.getBaseAddress())           // 프론트가 보내준 기본주소
                .detailAddress(requestDto.getDetailAddress())   // 유저가 직접 쓴 상세주소
                .isDefault(isDefault)
                .build();
        log.info("요청 isDefault = {}", requestDto.isDefaultAddress());
        addressRepository.save(address);
    }

    // 주소록 조회
    public List<UserAddressResponseDto> getAllAddress(Long userId) {

        List<Address> results = addressRepository.findByUser_UserIdOrderByIsDefaultDescCreatedAtDesc(userId);
        List<UserAddressResponseDto> responses = new ArrayList<>();

        for (Address address : results) {
            UserAddressResponseDto newResult = UserAddressResponseDto.builder()
                    .recipientName(address.getRecipientName())
                    .postalCode(address.getPostalCode())
                    .address(address.getAddress())
                    .detailAddress(address.getDetailAddress())
                    .defaultAddress(address.isDefault())
                    .build();

            responses.add(newResult);
        }
        return responses;
    }

}
