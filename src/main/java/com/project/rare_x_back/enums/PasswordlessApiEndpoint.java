package com.project.rare_x_back.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PasswordlessApiEndpoint {
    IS_AP("/ap/rest/auth/isAp"),
    JOIN_AP("/ap/rest/auth/joinAp"),
    WITHDRAWAL_AP("/ap/rest/auth/withdrawalAp"),
    GET_TOKEN_FOR_ONE_TIME("/ap/rest/auth/getTokenForOneTime"),
    GET_SP("/ap/rest/auth/getSp"),
    RESULT("/ap/rest/auth/result"),
    CANCEL("/ap/rest/auth/cancel");

    private final String path;
}
