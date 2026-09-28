package com.shopai.channel.service;

import com.shopai.channel.domain.ChannelConfig;
import com.shopai.channel.domain.ChannelType;
import com.shopai.channel.dto.ChannelDtos.ChannelConfigDto;
import com.shopai.channel.dto.ChannelDtos.UpdateChannelConfigRequest;
import com.shopai.channel.repository.ChannelConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChannelConfigService {

    private final ChannelConfigRepository configRepository;

    public ChannelConfigService(ChannelConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Transactional(readOnly = true)
    public List<ChannelConfigDto> getAllConfigs() {
        Map<ChannelType, ChannelConfig> existingMap = configRepository.findAll().stream()
                .collect(Collectors.toMap(ChannelConfig::getChannelType, c -> c));

        return Arrays.stream(ChannelType.values()).map(type -> {
            ChannelConfig config = existingMap.get(type);
            if (config != null) {
                return toDto(config);
            } else {
                return new ChannelConfigDto(null, type, false, true, Map.of(), Instant.now());
            }
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChannelConfigDto getConfig(ChannelType type) {
        return configRepository.findByChannelType(type)
                .map(this::toDto)
                .orElse(new ChannelConfigDto(null, type, false, true, Map.of(), Instant.now()));
    }

    @Transactional
    public ChannelConfigDto updateConfig(ChannelType type, UpdateChannelConfigRequest request) {
        ChannelConfig config = configRepository.findByChannelType(type)
                .orElseGet(() -> new ChannelConfig(type, request.enabled(), request.autoReplyEnabled()));

        config.setEnabled(request.enabled());
        config.setAutoReplyEnabled(request.autoReplyEnabled());
        if (request.configData() != null) {
            config.setConfigData(request.configData());
        }
        config.setUpdatedAt(Instant.now());

        ChannelConfig saved = configRepository.save(config);
        return toDto(saved);
    }

    public boolean isAutoReplyEnabled(ChannelType type) {
        return configRepository.findByChannelType(type)
                .map(ChannelConfig::isAutoReplyEnabled)
                .orElse(true);
    }

    private ChannelConfigDto toDto(ChannelConfig c) {
        return new ChannelConfigDto(
                c.getId(),
                c.getChannelType(),
                c.isEnabled(),
                c.isAutoReplyEnabled(),
                c.getConfigData(),
                c.getUpdatedAt()
        );
    }
}
