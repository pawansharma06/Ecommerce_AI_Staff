package com.shopai.channel.repository;

import com.shopai.channel.domain.ChannelMessage;
import com.shopai.channel.domain.ChannelType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChannelMessageRepository extends JpaRepository<ChannelMessage, UUID> {

    List<ChannelMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    List<ChannelMessage> findBySenderIdOrderByCreatedAtDesc(String senderId);

    @Query("""
        SELECT m FROM ChannelMessage m
        WHERE (:channelType IS NULL OR m.channelType = :channelType)
          AND (CAST(:search AS string) IS NULL OR LOWER(m.content) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(m.senderId) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(m.recipientId) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        ORDER BY m.createdAt DESC
    """)
    Page<ChannelMessage> findWithFilters(
            @Param("channelType") ChannelType channelType,
            @Param("search") String search,
            Pageable pageable
    );

    long countByChannelType(ChannelType channelType);
}
