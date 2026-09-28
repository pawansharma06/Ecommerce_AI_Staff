package com.shopai.common.health;

import com.shopai.llm.service.LlmService;
import com.shopai.tool.core.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);

    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final LlmService llmService;
    private final ToolRegistry toolRegistry;

    @Value("${app.version:1.0.0-SNAPSHOT}")
    private String appVersion;

    @Value("${app.name:shopai}")
    private String appName;

    public HealthService(
            JdbcTemplate jdbcTemplate,
            RedisTemplate<String, Object> redisTemplate,
            LlmService llmService,
            ToolRegistry toolRegistry
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.llmService = llmService;
        this.toolRegistry = toolRegistry;
    }

    public HealthResponse getHealth() {
        String dbStatus = checkDatabase();
        String redisStatus = checkRedis();
        String pgvectorStatus = checkPgVector();
        String activeProvider = checkLlmProvider();
        int toolsCount = toolRegistry != null ? toolRegistry.getToolCount() : 0;
        int channelsCount = 4; // WEB_CHAT, STOREFRONT_WIDGET, WHATSAPP, EMAIL

        boolean isHealthy = "UP".equals(dbStatus) && "UP".equals(redisStatus);
        String overallStatus = isHealthy ? "UP" : "DEGRADED";

        return new HealthResponse(
                overallStatus,
                appName,
                appVersion,
                dbStatus,
                redisStatus,
                pgvectorStatus,
                activeProvider,
                toolsCount,
                channelsCount
        );
    }

    private String checkDatabase() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (Exception ex) {
            log.warn("Database health check failed: {}", ex.getMessage());
            return "DOWN";
        }
    }

    private String checkPgVector() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM pg_extension WHERE extname = 'vector'", Integer.class
            );
            return (count != null && count > 0) ? "UP" : "DOWN";
        } catch (Exception ex) {
            return "UNKNOWN";
        }
    }

    private String checkRedis() {
        try {
            if (redisTemplate.getConnectionFactory() != null) {
                var conn = redisTemplate.getConnectionFactory().getConnection();
                conn.ping();
                conn.close();
                return "UP";
            }
            return "DOWN";
        } catch (Exception ex) {
            log.warn("Redis health check failed: {}", ex.getMessage());
            return "DOWN";
        }
    }

    private String checkLlmProvider() {
        try {
            if (llmService != null && llmService.getActiveProvider() != null) {
                return llmService.getActiveProvider().getProviderName();
            }
        } catch (Exception ignored) {}
        return "LOCAL_FALLBACK";
    }
}
