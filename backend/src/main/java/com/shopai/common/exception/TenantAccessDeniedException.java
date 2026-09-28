package com.shopai.common.exception;

import org.springframework.http.HttpStatus;

public class TenantAccessDeniedException extends ShopAiException {

    public TenantAccessDeniedException() {
        super("TENANT_ACCESS_DENIED",
              "Access denied: tenant boundary violation",
              HttpStatus.FORBIDDEN);
    }
}
