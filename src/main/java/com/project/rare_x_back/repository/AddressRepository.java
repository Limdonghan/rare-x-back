package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    //유저의 주소 개수 카운트
    int countByUser_UserId(Long userId);

    // 기본 배송지 해제
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.userId = :userId")
    void updateAllIsDefaultToFalse(Long userId);

    // 유저id로 주소 리스트 조회
    List<Address> findAddressesByUser_UserId(Long userId);

    // 유저id로 주소 리스트 조회 + 정렬 - 기본 배송지 true 젤 위에, 나머지는 등록 최신순
    List<Address> findByUser_UserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);

    List<Address> findByUser_UserIdAndAddressIdIn(Long userId, List<Long> addressIds);

    //유저 아이디와 주소록아이디로 주소록 조회
    Optional<Address> findByAddressIdAndUser_UserId(Long addressId, Long userId);

    // 유저의 id로 주소 조회, 그 중 기본 배송지 주소록 조회
    Optional <Address> findAddressesByUser_UserIdAndIsDefaultIsTrue(Long userId);

    // 최신 주소 조회
    Optional<Address> findTopByUser_UserIdOrderByCreatedAtDesc(Long userId);
    // 등록 최신순 조회
    List<Address> findTop2ByUser_UserIdOrderByCreatedAtDesc(Long userId);

    Optional<Address> findTopByUser_UserIdAndAddressIdNotOrderByCreatedAtDesc(Long userId, Long addressId);
}
