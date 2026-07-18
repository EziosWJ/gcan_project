package cn.ezios.baseapi.gcan.config;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExternalSourceConfig {

    boolean enabled;
    String baseUrl;
    String mineListEndpoint;
    String vehicleDataEndpoint;
    long pollIntervalMs;
    int connectTimeoutMs;
    int readTimeoutMs;
    int freshnessMultiplier;
}
