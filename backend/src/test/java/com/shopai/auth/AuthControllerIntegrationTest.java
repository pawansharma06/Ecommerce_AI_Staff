package com.shopai.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.auth.dto.LoginRequest;
import com.shopai.auth.dto.RegisterRequest;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void loginAndMeFlowWithHttpOnlyCookies() throws Exception {
        // 1. Login with bootstrapped default administrator
        LoginRequest loginReq = new LoginRequest("admin@shopai.dev", "AdminPassword123!");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(CookieUtils.ACCESS_TOKEN_COOKIE))
                .andExpect(cookie().httpOnly(CookieUtils.ACCESS_TOKEN_COOKIE, true))
                .andExpect(cookie().exists(CookieUtils.REFRESH_TOKEN_COOKIE))
                .andExpect(cookie().httpOnly(CookieUtils.REFRESH_TOKEN_COOKIE, true))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("admin@shopai.dev")))
                .andExpect(jsonPath("$.data.roles", hasItem("ADMIN")))
                .andExpect(jsonPath("$.data.permissions", hasItem("product.read")))
                .andReturn();

        Cookie accessTokenCookie = loginResult.getResponse().getCookie(CookieUtils.ACCESS_TOKEN_COOKIE);

        // 2. Access /api/v1/auth/me using HttpOnly Cookie
        mockMvc.perform(get("/api/v1/auth/me")
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("admin@shopai.dev")))
                .andExpect(jsonPath("$.data.roles", hasItem("ADMIN")));

        // 3. Logout clears cookies
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge(CookieUtils.ACCESS_TOKEN_COOKIE, 0));
    }
}
