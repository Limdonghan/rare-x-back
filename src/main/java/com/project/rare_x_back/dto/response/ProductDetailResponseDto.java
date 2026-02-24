package com.project.rare_x_back.dto.response;

import com.project.rare_x_back.dto.request.BidInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailResponseDto {
    private Long productId;
    private String productName;
    private String brandName;
    private String categoryName;
    private String productDescription;
    private int salePrice;
    private int buyPrice;
    private int retailPrice;
    private List<String> imageUrls;

    private List<BidInfo>  saleBidInfoList;
    private List<BidInfo> buyBidInfoList;

    private int wishCount;  // 현재 상품 찜수
    private boolean isLiked;    // 현재 상품 유저 찜 여부
    //본인 입찰인지 여부
    private boolean hasMyBuyBid;
    private boolean hasMySellBid;
    private List<Integer> myBuyBidPrices;
    private List<Integer> mySellBidPrices;

    private Long highestBuyBidId; // 즉시 판매 시 사용할 최고가 매수 입찰 ID
    private Long lowestSaleBidId; // 즉시 구매 시 사용할 최저가 판매 입찰 ID
}
