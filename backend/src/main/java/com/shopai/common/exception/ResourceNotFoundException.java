package com.shopai.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ShopAiException {

    public ResourceNotFoundException(String resourceType, String id) {
        super("RESOURCE_NOT_FOUND",
              resourceType + " not found: " + id,
              HttpStatus.NOT_FOUND);
    }
}
