package com.shopai.agent.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation_messages")
public class ConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @JsonIgnore
    private Conversation conversation;

    @Column(nullable = false, length = 20)
    private String role; // system, user, assistant, tool

    @Column(columnDefinition = "TEXT")
    private String content;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "tool_calls", columnDefinition = "jsonb")
    private String toolCalls;

    @Column(name = "tool_call_id", length = 100)
    private String toolCallId;

    @Column(name = "tool_name", length = 100)
    private String toolName;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "token_count", nullable = false)
    private Integer tokenCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ConversationMessage() {}

    public ConversationMessage(Conversation conversation, String role, String content) {
        this.conversation = conversation;
        this.role = role;
        this.content = content;
    }

    public static ConversationMessage user(Conversation conv, String content) {
        return new ConversationMessage(conv, "user", content);
    }

    public static ConversationMessage assistant(Conversation conv, String content) {
        return new ConversationMessage(conv, "assistant", content);
    }

    public static ConversationMessage assistantWithTools(Conversation conv, String content, String toolCallsJson) {
        ConversationMessage msg = new ConversationMessage(conv, "assistant", content);
        msg.setToolCalls(toolCallsJson);
        return msg;
    }

    public static ConversationMessage toolResult(Conversation conv, String toolCallId, String toolName, String content) {
        ConversationMessage msg = new ConversationMessage(conv, "tool", content);
        msg.setToolCallId(toolCallId);
        msg.setToolName(toolName);
        return msg;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getToolCalls() { return toolCalls; }
    public void setToolCalls(String toolCalls) { this.toolCalls = toolCalls; }

    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount != null ? tokenCount : 0; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
