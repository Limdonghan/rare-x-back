package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Address;
import com.project.rare_x_back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {

    //유저의 주소 갯수 카운트
    int countByUser_UserId(Long UserId);
    int countByUser(User user);

    // 기본 배송지 해제
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.userId = :userId")
    void updateAllIsDefaultToFalse(Long userId);

    // 유저id로 주소 리스트 조회
    List<Address> findAddressesByUser_UserId(Long userId);
    // 유저id로 주소 리스트 조회 + 정렬 - 기본 배송지 true 젤 위에, 나머지는 등록 최신순
    List<Address> findByUser_UserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);
}
