package com.shopai.channel.repository;

import com.shopai.channel.domain.ChannelConfig;
import com.shopai.channel.domain.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChannelConfigRepository extends JpaRepository<ChannelConfig, UUID> {
    Optional<ChannelConfig> findByChannelType(ChannelType channelType);
}
