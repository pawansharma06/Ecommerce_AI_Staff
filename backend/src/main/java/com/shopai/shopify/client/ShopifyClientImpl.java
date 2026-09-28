package com.shopai.shopify.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.common.exception.ShopAiException;
import com.shopai.shopify.service.ShopifyConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Component
public class ShopifyClientImpl implements ShopifyClient {

    private static final Logger log = LoggerFactory.getLogger(ShopifyClientImpl.class);

    private static final String SHOP_QUERY = """
        query GetShopDetails {
          shop {
            id
            name
            myshopifyDomain
            email
            currencyCode
            timezoneAbbreviation
            primaryDomain {
              url
              host
            }
          }
        }
        """;

    private final ObjectMapper objectMapper;
    private final ShopifyConfigService configService;
    private final HttpClient httpClient;

    public ShopifyClientImpl(ObjectMapper objectMapper, @Lazy ShopifyConfigService configService) {
        this.objectMapper = objectMapper;
        this.configService = configService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public GraphQLResponse executeGraphQL(
            String shopDomain,
            String accessToken,
            String apiVersion,
            String query,
            Map<String, Object> variables
    ) {
        String cleanDomain = normalizeDomain(shopDomain);
        String version = (apiVersion != null && !apiVersion.isBlank()) ? apiVersion : "2024-04";
        String endpoint = String.format("https://%s/admin/api/%s/graphql.json", cleanDomain, version);

        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("query", query);
            if (variables != null && !variables.isEmpty()) {
                payloadMap.put("variables", variables);
            }
            String requestBody = objectMapper.writeValueAsString(payloadMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("X-Shopify-Access-Token", accessToken)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(20))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                log.warn("Shopify API rate limited (429) on store {}. Retrying after backoff...", cleanDomain);
                Thread.sleep(1000);
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            }

            if (response.statusCode() >= 400 && response.statusCode() != 422) {
                log.error("Shopify HTTP error {}: {}", response.statusCode(), response.body());
                return GraphQLResponse.error(List.of("Shopify HTTP Error: " + response.statusCode()));
            }

            JsonNode rootNode = objectMapper.readTree(response.body());

            List<String> errors = new ArrayList<>();
            if (rootNode.has("errors")) {
                JsonNode errNode = rootNode.get("errors");
                if (errNode.isArray()) {
                    for (JsonNode e : errNode) {
                        errors.add(e.has("message") ? e.get("message").asText() : e.asText());
                    }
                } else if (errNode.isTextual()) {
                    errors.add(errNode.asText());
                }
            }

            JsonNode dataNode = rootNode.get("data");

            int actualCost = 0;
            double available = 1000.0;
            double restoreRate = 50.0;

            if (rootNode.has("extensions") && rootNode.get("extensions").has("cost")) {
                JsonNode costNode = rootNode.get("extensions").get("cost");
                actualCost = costNode.has("actualQueryCost") ? costNode.get("actualQueryCost").asInt() : 0;
                if (costNode.has("throttleStatus")) {
                    JsonNode throttle = costNode.get("throttleStatus");
                    available = throttle.has("currentlyAvailable") ? throttle.get("currentlyAvailable").asDouble() : 1000.0;
                    restoreRate = throttle.has("restoreRate") ? throttle.get("restoreRate").asDouble() : 50.0;
                }
            }

            return new GraphQLResponse(dataNode, errors, actualCost, available, restoreRate);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ShopAiException("SHOPIFY_REQUEST_INTERRUPTED", "Shopify request was interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IOException e) {
            log.error("Failed to execute Shopify GraphQL request to {}: {}", cleanDomain, e.getMessage());
            return GraphQLResponse.error(List.of("Network error connecting to Shopify: " + e.getMessage()));
        }
    }

    @Override
    public GraphQLResponse executeGraphQL(String query, Map<String, Object> variables) {
        String domain = configService.getActiveShopDomain();
        String token = configService.getActiveAccessToken();
        String version = configService.getActiveApiVersion();

        if (domain == null || token == null) {
            throw new ShopAiException("SHOPIFY_NOT_CONFIGURED", "Shopify credentials are not configured", HttpStatus.BAD_REQUEST);
        }

        return executeGraphQL(domain, token, version, query, variables);
    }

    @Override
    public Map<String, Object> getShopDetails(String shopDomain, String accessToken, String apiVersion) {
        GraphQLResponse response = executeGraphQL(shopDomain, accessToken, apiVersion, SHOP_QUERY, Collections.emptyMap());
        if (response.hasErrors() || response.data() == null || !response.data().has("shop")) {
            String errMsg = response.hasErrors() ? String.join(", ", response.errors()) : "No shop data returned";
            throw new ShopAiException("SHOPIFY_FETCH_FAILED", "Failed to fetch shop details: " + errMsg, HttpStatus.BAD_REQUEST);
        }

        JsonNode shopNode = response.data().get("shop");
        Map<String, Object> details = new HashMap<>();
        details.put("id", shopNode.has("id") ? shopNode.get("id").asText() : "");
        details.put("name", shopNode.has("name") ? shopNode.get("name").asText() : "");
        details.put("myshopifyDomain", shopNode.has("myshopifyDomain") ? shopNode.get("myshopifyDomain").asText() : shopDomain);
        details.put("email", shopNode.has("email") ? shopNode.get("email").asText() : "");
        details.put("currencyCode", shopNode.has("currencyCode") ? shopNode.get("currencyCode").asText() : "USD");
        details.put("timezoneAbbreviation", shopNode.has("timezoneAbbreviation") ? shopNode.get("timezoneAbbreviation").asText() : "UTC");

        return details;
    }

    @Override
    public boolean testConnection(String shopDomain, String accessToken, String apiVersion) {
        try {
            getShopDetails(shopDomain, accessToken, apiVersion);
            return true;
        } catch (Exception e) {
            log.warn("Shopify connection test failed for {}: {}", shopDomain, e.getMessage());
            return false;
        }
    }

    private String normalizeDomain(String domain) {
        if (domain == null) return "";
        String clean = domain.trim().toLowerCase();
        clean = clean.replace("https://", "").replace("http://", "");
        if (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }
}
