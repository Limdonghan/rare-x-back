package com.project.rare_x_back.repository;

public interface ProductRankingProjection {
    Long getProductId();

    String getProductName();

    String getThumbnailUrl();

    Long getDealCount();

    String getBrandName();

    Integer getLowestPrice();

    Long getBrandId();

    Long getCategoryId();
}
