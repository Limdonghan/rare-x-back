package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.request.AddressRegisterRequestDto;
import com.project.rare_x_back.dto.request.DefaultAddressUpdateRequestDto;
import com.project.rare_x_back.dto.request.UserAddressUpdateRequestDto;
import com.project.rare_x_back.dto.response.JusoResponseDto;
import com.project.rare_x_back.dto.response.UserAddressResponseDto;
import com.project.rare_x_back.entity.Address;
import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.enums.BidStatus;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.AddressRepository;
import com.project.rare_x_back.repository.BuyBidRepository;
import com.project.rare_x_back.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressService {

    private final WebClient jusoWebClient;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final BuyBidRepository buyBidRepository;

    @Value("${juso.api.key}")
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

        // 유저의 현재 주소 개수 조회
        int addressCount = addressRepository.countByUser_UserId(userId);
        // 한 유저당 등록 가능 배송지는 최대 10개 정책
        if (addressCount >= 10) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "배송지는 최대 10개까지 등록 가능 합니다.");
        }

        User user = userRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST, "존재하지 않는 유저입니다."));

        // 주소 갯수가 0이면 -> 정책에 맞게 최초 등록 주소는 기본배송지로 설정
        // 주소가 이미 있는데 이번에 등록하는 주소를 기본으로 설정 -> 기존 기본 배송 해제
        boolean isDefault = requestDto.isDefaultAddress();
        if (addressCount == 0) {
            isDefault = true;
        } else if (isDefault) {
            addressRepository.updateAllIsDefaultToFalse(userId);
        }
        // 엔티티 생성 및 저장
        Address address = Address.builder()
                .user(userRepository.getReferenceById(userId))
                .recipientName(requestDto.getRecipientName())   // 유저가 직접 쓴 받는이
                .postalCode(requestDto.getPostalCode())         // 프론트가 보내준 우편번호
                .address(requestDto.getBaseAddress())           // 프론트가 보내준 기본주소
                .detailAddress(requestDto.getDetailAddress())   // 유저가 직접 쓴 상세주소
                .nickname(requestDto.getNickname())             // 유저가 직접 쓴 배송지별칭
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
                    .addressId(address.getAddressId())
                    .recipientName(address.getRecipientName())
                    .postalCode(address.getPostalCode())
                    .address(address.getAddress())
                    .detailAddress(address.getDetailAddress())
                    .nickname(address.getNickname())
                    .defaultAddress(address.isDefault())
                    .createdAt(address.getCreatedAt())
                    .updatedAt(address.getUpdatedAt())
                    .build();

            responses.add(newResult);
        }
        return responses;
    }

    // 주소 수정
    // 유저 정보와 수정할 주소록아이디를 받아와야함.
    // 수정 가능한 정보는 유저가 직접 입력한 상세 주소와 받는이, 그리고 기본 배송지 여부 (배송지 여부는 정책상 별도의 api 따로 설정 해야한
    @Transactional
    public void updateAddress (Long userId, Long addressId, UserAddressUpdateRequestDto requestDto) {
        // 수정할 유저의 주소록 정보 찾기
        Address changeAddress = addressRepository.findByAddressIdAndUser_UserId(addressId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST, "지정된 사용자에 대한 주소를 찾을 수 없습니다."));
        changeAddress.updateAddress(requestDto.getRecipientName(), requestDto.getDetailAddress(), requestDto.getNickname());
    }

    // 기본 배송지 여부 수정
    // 기본배송지 수정시 바꾸려는 주소가 기본인데 해제 -> 최신 등록 주소가 자동으로 기본
    // 만약 주소가 지금 이거 하나라면 기본배송지 해제 못함
    // 만약 기존 기본 배송지는 기본배송지 여부 해제해야함
    // findAddressesByUser_UserIdAndIsDefaultIsTrue
    @Transactional
    public void updateDefaultAddress (Long userId, Long addressId, DefaultAddressUpdateRequestDto requestDto) {

        Address changeAddress =  addressRepository.findByAddressIdAndUser_UserId(addressId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST, "지정된 사용자에 대한 주소를 찾을 수 없습니다."));

        // 요청
        boolean isDefault = requestDto.isDefaultAddress();
        // 유저의 주소록 개수
        int addressCount = addressRepository.countByUser_UserId(userId);
        // 유저의 기존 기본 배송지
        Optional<Address> currentDefaultAddr = addressRepository.findAddressesByUser_UserIdAndIsDefaultIsTrue(userId);
        // 유저의 제일 최신 배송지 (2개 -> 바꾸려는 주소가 제일 최신 일 수 있어서..)
        List<Address> latestAddr = addressRepository.findTop2ByUser_UserIdOrderByCreatedAtDesc(userId);

        // 유저가 기본으로 설정 요청 했을 때 - > isDefault가 true인 경우, false는 if문 그대로 건너뜀..
        if (isDefault) {
            // 이미 기본배송지면 할 일 없어서 메소드 종료
            if (changeAddress.isDefault()) return;
            // 기존의 기본 배송지가 있으면 해제
            currentDefaultAddr.ifPresent(Address::unsetDefault);
            changeAddress.setAsDefault();
            return;
        }
        // 여기서 부터 isDefault가 false인 경우, 해제 요청

        // 유저가 해제해달라고 했는데, 이미 기본 배송지도 아니었을 경우,
        if (!changeAddress.isDefault()) return;

        // 유저의 주소 개수가 1개이하 이면 기본 배송지 정책 적용(최소1개, 최초등록한 주소 = 자동 기본배송지)
        if (addressCount <= 1) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "기본 배송지는 1개 필수 입니다.");
        }
        changeAddress.unsetDefault(); // 아니면 해제

        // 요청 주소가 기본 배송지였고 그걸 해제 했다면 -> 대체 기본 배송지 선택
        Address newDefaultAddr = addressRepository.findTopByUser_UserIdAndAddressIdNotOrderByCreatedAtDesc(userId, addressId)
                        .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST,"기본 배송지를 대체할 주소를 찾을 수 없습니다."));
        
        newDefaultAddr.setAsDefault();
    }

    // 주소 삭제
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {

        Address deleteAddress = addressRepository.findByAddressIdAndUser_UserId(addressId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST, "지정된 사용자에 대한 주소를 찾을 수 없습니다."));

        // OPEN 입찰이 참조 중이면 삭제 거부
        boolean hasOpenBids = buyBidRepository.existsByAddressIdAndStatus(addressId, BidStatus.OPEN);
        if (hasOpenBids) {
            throw new CustomException(ErrorCode.ADDRESS_IN_USE_BY_BID);
        }

        // 종료된 입찰의 address_id를 NULL로 처리
        buyBidRepository.nullifyAddressByAddressId(addressId,
                List.of(BidStatus.MATCHED, BidStatus.CANCELED, BidStatus.EXPIRED));

        boolean wasDefault = deleteAddress.isDefault();
        // 주소 삭제 — FK 레이스 컨디션 방어
        try {
            addressRepository.delete(deleteAddress);
            addressRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.ADDRESS_IN_USE_BY_BID);
        }

        // 삭제한 배송지가 기본 배송지였다면
        if (wasDefault) {
            Optional<Address> newDefaultAddr = addressRepository
                    .findTopByUser_UserIdAndAddressIdNotOrderByCreatedAtDesc(userId, addressId);
            newDefaultAddr.ifPresent(Address::setAsDefault);
        }
    }

}
