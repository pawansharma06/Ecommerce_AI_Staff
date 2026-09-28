package com.shopai.shopify.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.List;

public record GraphQLResponse(
        JsonNode data,
        List<String> errors,
        int actualQueryCost,
        double currentlyAvailablePoints,
        double restoreRate
) {
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public static GraphQLResponse success(JsonNode data, int cost, double available, double restoreRate) {
        return new GraphQLResponse(data, Collections.emptyList(), cost, available, restoreRate);
    }

    public static GraphQLResponse error(List<String> errors) {
        return new GraphQLResponse(null, errors, 0, 0, 0);
    }
}
