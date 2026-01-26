package com.project.rare_x_back.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BidInfo {
    private int price;   // 가격
    private long quantity; // 수량
}
