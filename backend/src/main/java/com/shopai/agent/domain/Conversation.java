package com.shopai.agent.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title = "New Conversation";

    @Column(name = "agent_type", nullable = false, length = 50)
    private String agentType = "CUSTOMER_SUPPORT"; // CUSTOMER_SUPPORT, ADMIN_COPILOT

    @Column(nullable = false, length = 50)
    private String channel = "WEB_CHAT"; // WEB_CHAT, ADMIN_COPILOT, STOREFRONT_WIDGET

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Column(name = "customer_id")
    private Long customerId;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ConversationMessage> messages = new ArrayList<>();

    public Conversation() {}

    public Conversation(String title, String agentType, String channel, String customerEmail) {
        this.title = title != null ? title : "New Conversation";
        this.agentType = agentType != null ? agentType : "CUSTOMER_SUPPORT";
        this.channel = channel != null ? channel : "WEB_CHAT";
        this.customerEmail = customerEmail;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAgentType() { return agentType; }
    public void setAgentType(String agentType) { this.agentType = agentType; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<ConversationMessage> getMessages() { return messages; }
    public void setMessages(List<ConversationMessage> messages) { this.messages = messages; }

    public void addMessage(ConversationMessage msg) {
        messages.add(msg);
        msg.setConversation(this);
        this.updatedAt = Instant.now();
    }
}
