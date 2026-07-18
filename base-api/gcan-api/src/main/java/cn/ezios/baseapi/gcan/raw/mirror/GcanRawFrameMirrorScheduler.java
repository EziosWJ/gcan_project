package cn.ezios.baseapi.gcan.raw.mirror;

import cn.ezios.baseapi.gcan.common.BoxIdUtil;
import cn.ezios.baseapi.gcan.config.GcanProperties;
import cn.ezios.baseapi.gcan.raw.RawCanFrame;
import cn.ezios.baseapi.gcan.raw.RawCanFrameSnapshotStore;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class GcanRawFrameMirrorScheduler {

    private final GcanProperties.Mirror properties;
    private final GcanRawFrameSource source;
    private final RawCanFrameSnapshotStore snapshotStore;
    private final Map<String, MirrorBoxStatus> statuses = new ConcurrentHashMap<>();

    public GcanRawFrameMirrorScheduler(GcanProperties properties,
                                       GcanRawFrameSource source,
                                       RawCanFrameSnapshotStore snapshotStore) {
        this.properties = properties.getMirror();
        this.source = source;
        this.snapshotStore = snapshotStore;
    }

    @PostConstruct
    public void validateConfiguration() {
        if (!properties.isEnabled()) {
            return;
        }
        validateBaseUrl();
        if (properties.getPollIntervalMs() <= 0) {
            throw new IllegalStateException("GCAN 原始 CAN 镜像轮询周期必须大于 0");
        }
        if (properties.getConnectTimeoutMs() <= 0 || properties.getReadTimeoutMs() <= 0) {
            throw new IllegalStateException("GCAN 原始 CAN 镜像连接和读取超时必须大于 0");
        }
        List<String> boxIds = normalizedBoxIds();
        if (boxIds.isEmpty()) {
            throw new IllegalStateException("GCAN 原始 CAN 镜像开启时必须配置至少一个盒子 ID");
        }
        normalizedCanIds();
        log.info("GCAN raw CAN mirror enabled: endpoint={}, boxIds={}, canIds={}, pollIntervalMs={}",
                properties.getEndpoint(), boxIds, normalizedCanIds(), properties.getPollIntervalMs());
    }

    @Scheduled(fixedDelayString = "${gcan.mirror.poll-interval-ms:1000}")
    public void mirrorCurrentFrames() {
        if (!properties.isEnabled()) {
            return;
        }
        Set<String> canIds = normalizedCanIds();
        for (String boxId : normalizedBoxIds()) {
            mirrorBox(boxId, canIds);
        }
    }

    private void mirrorBox(String boxId, Set<String> canIds) {
        try {
            List<RawCanFrame> frames = source.load(boxId).stream()
                    .filter(frame -> canIds.isEmpty() || canIds.contains(frame.getCanId()))
                    .toList();
            snapshotStore.replaceFrames(boxId, canIds, frames);
            recordSuccess(boxId, frames.size());
        } catch (Exception exception) {
            recordFailure(boxId, exception);
        }
    }

    private void recordSuccess(String boxId, int frameCount) {
        MirrorBoxStatus status = statuses.computeIfAbsent(boxId, ignored -> new MirrorBoxStatus());
        int previousFailures = status.consecutiveFailures;
        boolean wasNoData = status.hasSuccess && status.lastFrameCount == 0;
        status.hasSuccess = true;
        status.lastSuccessAt = LocalDateTime.now();
        status.lastFrameCount = frameCount;
        status.consecutiveFailures = 0;

        if (previousFailures > 0) {
            log.info("GCAN raw CAN mirror recovered: boxId={}, frameCount={}, lastSuccessAt={}, previousFailures={}",
                    boxId, frameCount, status.lastSuccessAt, previousFailures);
        } else if (frameCount > 0) {
            log.debug("GCAN raw CAN mirror refreshed: boxId={}, frameCount={}, lastSuccessAt={}",
                    boxId, frameCount, status.lastSuccessAt);
        }
        if (frameCount == 0 && !wasNoData) {
            log.warn("GCAN raw CAN mirror has no current frames: boxId={}, lastSuccessAt={}",
                    boxId, status.lastSuccessAt);
        }
    }

    private void recordFailure(String boxId, Exception exception) {
        MirrorBoxStatus status = statuses.computeIfAbsent(boxId, ignored -> new MirrorBoxStatus());
        status.consecutiveFailures++;
        log.warn("GCAN raw CAN mirror failed: boxId={}, lastSuccessAt={}, consecutiveFailures={}, reason={}",
                boxId, status.lastSuccessAt, status.consecutiveFailures, exception.getMessage());
    }

    private void validateBaseUrl() {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new IllegalStateException("GCAN 原始 CAN 镜像开启时必须配置线上 Base URL");
        }
        try {
            URI uri = URI.create(properties.getBaseUrl());
            if (!StringUtils.hasText(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
                throw new IllegalArgumentException("缺少协议或主机");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("GCAN 原始 CAN 镜像线上 Base URL 无效", exception);
        }
    }

    private List<String> normalizedBoxIds() {
        if (properties.getBoxIds() == null) {
            return List.of();
        }
        return properties.getBoxIds().stream()
                .filter(StringUtils::hasText)
                .map(value -> {
                    try {
                        return BoxIdUtil.normalizeHex(value);
                    } catch (RuntimeException exception) {
                        throw new IllegalStateException("GCAN 原始 CAN 镜像盒子 ID 无效: " + value, exception);
                    }
                })
                .distinct()
                .toList();
    }

    private Set<String> normalizedCanIds() {
        if (properties.getCanIds() == null) {
            return Set.of();
        }
        LinkedHashSet<String> canIds = new LinkedHashSet<>();
        properties.getCanIds().stream()
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toUpperCase())
                .forEach(canIds::add);
        return Collections.unmodifiableSet(canIds);
    }

    private static class MirrorBoxStatus {
        private boolean hasSuccess;
        private LocalDateTime lastSuccessAt;
        private int lastFrameCount;
        private int consecutiveFailures;
    }
}
