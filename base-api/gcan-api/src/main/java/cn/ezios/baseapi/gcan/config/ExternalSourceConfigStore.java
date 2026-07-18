package cn.ezios.baseapi.gcan.config;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class ExternalSourceConfigStore {

    private static final String PREFIX = "gcan.external.";

    private final GcanProperties.External defaults;
    private final ExternalSourceConfigMapper mapper;
    private final AtomicReference<ExternalSourceConfig> current = new AtomicReference<>();

    public ExternalSourceConfigStore(GcanProperties properties, ExternalSourceConfigMapper mapper) {
        this.defaults = properties.getExternal();
        this.mapper = mapper;
        this.current.set(fromDefaults());
    }

    @PostConstruct
    public void loadOnStartup() {
        refresh();
    }

    @Scheduled(fixedDelayString = "${gcan.external.config-refresh-interval-ms:10000}")
    public void refresh() {
        try {
            Map<String, String> values = new HashMap<>();
            for (ExternalSourceConfigRow row : mapper.selectActive()) {
                if (row != null && StringUtils.hasText(row.getConfigKey())) {
                    values.put(row.getConfigKey().trim(), row.getConfigValue());
                }
            }
            current.set(from(values));
        } catch (RuntimeException exception) {
            log.warn("外部车辆数据源配置刷新失败，继续使用旧配置", exception);
        }
    }

    public ExternalSourceConfig current() {
        return current.get();
    }

    private ExternalSourceConfig fromDefaults() {
        return ExternalSourceConfig.builder()
                .enabled(defaults.isEnabled())
                .baseUrl(defaults.getBaseUrl())
                .mineListEndpoint(defaults.getMineListEndpoint())
                .vehicleDataEndpoint(defaults.getVehicleDataEndpoint())
                .pollIntervalMs(defaults.getPollIntervalMs())
                .connectTimeoutMs(defaults.getConnectTimeoutMs())
                .readTimeoutMs(defaults.getReadTimeoutMs())
                .freshnessMultiplier(defaults.getFreshnessMultiplier())
                .build();
    }

    private ExternalSourceConfig from(Map<String, String> values) {
        ExternalSourceConfig fallback = fromDefaults();
        return ExternalSourceConfig.builder()
                .enabled(booleanValue(values, "enabled", fallback.isEnabled()))
                .baseUrl(textValue(values, "base-url", fallback.getBaseUrl()))
                .mineListEndpoint(textValue(values, "mine-list-endpoint", fallback.getMineListEndpoint()))
                .vehicleDataEndpoint(textValue(values, "vehicle-data-endpoint", fallback.getVehicleDataEndpoint()))
                .pollIntervalMs(longValue(values, "poll-interval-ms", fallback.getPollIntervalMs(), 1000L))
                .connectTimeoutMs((int) longValue(values, "connect-timeout-ms", fallback.getConnectTimeoutMs(), 100L))
                .readTimeoutMs((int) longValue(values, "read-timeout-ms", fallback.getReadTimeoutMs(), 100L))
                .freshnessMultiplier((int) longValue(values, "freshness-multiplier", fallback.getFreshnessMultiplier(), 1L))
                .build();
    }

    private String textValue(Map<String, String> values, String key, String fallback) {
        String value = values.get(PREFIX + key);
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private boolean booleanValue(Map<String, String> values, String key, boolean fallback) {
        String value = values.get(PREFIX + key);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }

    private long longValue(Map<String, String> values, String key, long fallback, long minimum) {
        String value = values.get(PREFIX + key);
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return Math.max(minimum, Long.parseLong(value.trim()));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
