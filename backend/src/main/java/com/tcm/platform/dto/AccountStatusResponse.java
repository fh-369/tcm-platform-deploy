package com.tcm.platform.dto;

import com.tcm.platform.entity.Account;

public record AccountStatusResponse(Long id, String username, Boolean enabled) {

    public static AccountStatusResponse from(Account account) {
        return new AccountStatusResponse(account.getId(), account.getUsername(), account.getEnabled());
    }
}
