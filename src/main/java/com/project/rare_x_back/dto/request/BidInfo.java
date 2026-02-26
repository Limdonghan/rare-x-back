package com.project.rare_x_back.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BidInfo {
    private int price;   // 가격
    private long quantity; // 수량

    @JsonProperty("isMine")
    private boolean isMine;

    @JsonProperty("isStorageSale")
    private boolean isStorageSale = false;

    private long storageQuantity;

    public BidInfo(int price, long quantity, boolean isMine) {
        this.price = price;
        this.quantity = quantity;
        this.isMine = isMine;
        this.isStorageSale = false;
        this.storageQuantity=0;
    }
}
