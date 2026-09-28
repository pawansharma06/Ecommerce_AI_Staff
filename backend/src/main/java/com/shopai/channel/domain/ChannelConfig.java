package com.shopai.channel.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "channel_configs")
public class ChannelConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 64, unique = true)
    private ChannelType channelType;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "auto_reply_enabled", nullable = false)
    private boolean autoReplyEnabled = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> configData = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ChannelConfig() {}

    public ChannelConfig(ChannelType channelType, boolean enabled, boolean autoReplyEnabled) {
        this.channelType = channelType;
        this.enabled = enabled;
        this.autoReplyEnabled = autoReplyEnabled;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public ChannelType getChannelType() { return channelType; }
    public void setChannelType(ChannelType channelType) { this.channelType = channelType; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isAutoReplyEnabled() { return autoReplyEnabled; }
    public void setAutoReplyEnabled(boolean autoReplyEnabled) { this.autoReplyEnabled = autoReplyEnabled; }

    public Map<String, Object> getConfigData() { return configData; }
    public void setConfigData(Map<String, Object> configData) { this.configData = configData; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
