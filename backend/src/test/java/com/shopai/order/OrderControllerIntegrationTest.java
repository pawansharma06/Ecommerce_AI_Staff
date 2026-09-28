package com.shopai.order;

import com.shopai.auth.security.CookieUtils;
import com.shopai.auth.security.JwtService;
import com.shopai.order.domain.AbandonedCheckout;
import com.shopai.order.domain.Order;
import com.shopai.order.repository.AbandonedCheckoutRepository;
import com.shopai.order.repository.OrderRepository;
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
class OrderControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OrderRepository orderRepository;
    @Autowired private AbandonedCheckoutRepository abandonedCheckoutRepository;
    @Autowired private JwtService jwtService;

    private Cookie adminAuthCookie;

    @BeforeEach
    void setUp() {
        String token = jwtService.generateAccessToken(UUID.randomUUID(), "admin@shopai.dev", List.of("ADMIN"), List.of("order.read"));
        adminAuthCookie = new Cookie(CookieUtils.ACCESS_TOKEN_COOKIE, token);

        Order o = new Order(555L, "1005", "#1005");
        o.setFinancialStatus("PAID");
        o.setFulfillmentStatus("UNFULFILLED");
        o.setTotalPrice(new BigDecimal("99.00"));
        orderRepository.save(o);

        AbandonedCheckout a = new AbandonedCheckout(888L, "test@shopai.dev", new BigDecimal("150.00"));
        abandonedCheckoutRepository.save(a);
    }

    @Test
    void testGetOrdersAndAbandonedCheckouts() throws Exception {
        mockMvc.perform(get("/api/v1/orders")
                        .cookie(adminAuthCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/api/v1/orders/stats")
                        .cookie(adminAuthCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalOrders", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/v1/abandoned-checkouts")
                        .cookie(adminAuthCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))));
    }
}
