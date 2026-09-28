package com.shopai.common.health;

import com.shopai.llm.provider.LlmProvider;
import com.shopai.llm.service.LlmService;
import com.shopai.tool.core.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private RedisConnection redisConnection;

    @Mock
    private LlmService llmService;

    @Mock
    private ToolRegistry toolRegistry;

    @Mock
    private LlmProvider llmProvider;

    private HealthService healthService;

    @BeforeEach
    void setUp() {
        healthService = new HealthService(jdbcTemplate, redisTemplate, llmService, toolRegistry);
        ReflectionTestUtils.setField(healthService, "appVersion", "1.0.0-TEST");
        ReflectionTestUtils.setField(healthService, "appName", "shopai");
    }

    @Test
    void returnsUpWhenAllDependenciesAreHealthy() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("pg_extension"), eq(Integer.class))).thenReturn(1);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(llmService.getActiveProvider()).thenReturn(llmProvider);
        when(llmProvider.getProviderName()).thenReturn("OPENAI");
        when(toolRegistry.getToolCount()).thenReturn(11);

        HealthResponse response = healthService.getHealth();

        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.database()).isEqualTo("UP");
        assertThat(response.redis()).isEqualTo("UP");
        assertThat(response.pgvector()).isEqualTo("UP");
        assertThat(response.activeLlmProvider()).isEqualTo("OPENAI");
        assertThat(response.registeredToolsCount()).isEqualTo(11);
        assertThat(response.application()).isEqualTo("shopai");
        assertThat(response.version()).isEqualTo("1.0.0-TEST");
    }

    @Test
    void returnsDegradedWhenDatabaseIsDown() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new RuntimeException("Connection refused"));
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);

        HealthResponse response = healthService.getHealth();

        assertThat(response.status()).isEqualTo("DEGRADED");
        assertThat(response.database()).isEqualTo("DOWN");
        assertThat(response.redis()).isEqualTo("UP");
    }

    @Test
    void returnsDegradedWhenRedisIsDown() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenThrow(new RuntimeException("Redis unavailable"));

        HealthResponse response = healthService.getHealth();

        assertThat(response.status()).isEqualTo("DEGRADED");
        assertThat(response.database()).isEqualTo("UP");
        assertThat(response.redis()).isEqualTo("DOWN");
    }
}
