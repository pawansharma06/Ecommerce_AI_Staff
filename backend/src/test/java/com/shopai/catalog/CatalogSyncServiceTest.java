package com.shopai.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.catalog.domain.Product;
import com.shopai.catalog.repository.CollectionRepository;
import com.shopai.catalog.repository.ProductRepository;
import com.shopai.catalog.repository.ProductVariantRepository;
import com.shopai.catalog.service.CatalogSyncService;
import com.shopai.shopify.client.GraphQLResponse;
import com.shopai.shopify.client.ShopifyClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogSyncServiceTest {

    @Mock private ShopifyClient shopifyClient;
    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private CollectionRepository collectionRepository;
    @Mock private com.shopai.rag.service.KnowledgeIndexService knowledgeIndexService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CatalogSyncService catalogSyncService;

    @Test
    void testPerformFullCatalogSync() throws Exception {
        String json = """
        {
          "data": {
            "products": {
              "pageInfo": { "hasNextPage": false, "endCursor": null },
              "edges": [
                {
                  "node": {
                    "id": "gid://shopify/Product/987654321",
                    "title": "Snowboard Supreme",
                    "handle": "snowboard-supreme",
                    "descriptionHtml": "<p>Awesome board</p>",
                    "vendor": "ShopAI Snow",
                    "productType": "Snowboards",
                    "status": "ACTIVE",
                    "tags": ["winter", "sports"],
                    "totalInventory": 15,
                    "variants": {
                      "edges": [
                        {
                          "node": {
                            "id": "gid://shopify/ProductVariant/11223344",
                            "title": "156cm",
                            "sku": "SNOW-156",
                            "price": "599.99",
                            "inventoryQuantity": 15
                          }
                        }
                      ]
                    },
                    "collections": {
                      "edges": []
                    }
                  }
                }
              ]
            }
          }
        }
        """;

        JsonNode rootNode = objectMapper.readTree(json);
        GraphQLResponse gqlResponse = GraphQLResponse.success(rootNode.path("data"), 10, 990, 50);

        when(shopifyClient.executeGraphQL(anyString(), any(Map.class))).thenReturn(gqlResponse);
        when(productRepository.findByShopifyProductId(987654321L)).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        int processed = catalogSyncService.performFullCatalogSync(null);

        assertThat(processed).isEqualTo(1);
        verify(productRepository).save(any(Product.class));
        verify(productVariantRepository).save(any());
    }
}
