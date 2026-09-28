package com.shopai.catalog;

import com.shopai.auth.security.CookieUtils;
import com.shopai.auth.security.JwtService;
import com.shopai.catalog.domain.Product;
import com.shopai.catalog.domain.ProductVariant;
import com.shopai.catalog.repository.ProductRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProductRepository productRepository;
    @Autowired private JwtService jwtService;

    private Cookie adminAuthCookie;

    @BeforeEach
    void setUp() {
        String token = jwtService.generateAccessToken(UUID.randomUUID(), "admin@shopai.dev", List.of("ADMIN"), List.of("product.read", "product.update"));
        adminAuthCookie = new Cookie(CookieUtils.ACCESS_TOKEN_COOKIE, token);

        Product p = new Product(10001L, "Sample Hoodie", "sample-hoodie");
        p.setVendor("ShopAI");
        p.setStatus("ACTIVE");
        p.setTotalInventory(20);

        ProductVariant v = new ProductVariant(p, 20001L, "L", new BigDecimal("49.99"));
        v.setInventoryQuantity(20);
        p.addVariant(v);

        productRepository.save(p);
    }

    @Test
    void testGetProductsAndStats() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .cookie(adminAuthCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].title", notNullValue()));

        mockMvc.perform(get("/api/v1/products/stats")
                        .cookie(adminAuthCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalProducts", greaterThanOrEqualTo(1)));
    }
}
