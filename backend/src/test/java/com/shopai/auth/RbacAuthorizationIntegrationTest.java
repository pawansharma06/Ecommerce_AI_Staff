package com.shopai.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.auth.dto.CreateRoleRequest;
import com.shopai.auth.dto.LoginRequest;
import com.shopai.auth.security.CookieUtils;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RbacAuthorizationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void verifiesRbacRoleAndPermissionEnforcement() throws Exception {
        // 1. Authenticate as Admin
        LoginRequest loginReq = new LoginRequest("admin@shopai.dev", "AdminPassword123!");
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie adminCookie = res.getResponse().getCookie(CookieUtils.ACCESS_TOKEN_COOKIE);

        // 2. ADMIN can create custom role
        CreateRoleRequest customRoleReq = new CreateRoleRequest(
                "SUPPORT_AGENT", "Customer support agent role", Set.of("customer.read", "order.read")
        );
        mockMvc.perform(post("/api/v1/roles")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customRoleReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name", is("SUPPORT_AGENT")))
                .andExpect(jsonPath("$.data.permissions", hasItems("customer.read", "order.read")));

        // 3. ADMIN can list all system permissions
        mockMvc.perform(get("/api/v1/roles/permissions").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(16))))
                .andExpect(jsonPath("$.data[*].name", hasItem("product.read")))
                .andExpect(jsonPath("$.data[*].name", hasItem("order.cancel")));

        // 4. Unauthenticated request to /api/v1/users is rejected
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }
}
