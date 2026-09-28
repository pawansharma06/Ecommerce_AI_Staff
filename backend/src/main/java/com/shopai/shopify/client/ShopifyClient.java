package com.shopai.shopify.client;

import java.util.Map;

public interface ShopifyClient {

    GraphQLResponse executeGraphQL(
            String shopDomain,
            String accessToken,
            String apiVersion,
            String query,
            Map<String, Object> variables
    );

    GraphQLResponse executeGraphQL(String query, Map<String, Object> variables);

    Map<String, Object> getShopDetails(String shopDomain, String accessToken, String apiVersion);

    boolean testConnection(String shopDomain, String accessToken, String apiVersion);
}
