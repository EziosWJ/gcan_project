package cn.ezios.baseapi.gcan.external;

import cn.ezios.baseapi.common.model.ApiResponse;
import cn.ezios.baseapi.gcan.config.ExternalSourceConfig;
import cn.ezios.baseapi.gcan.config.ExternalSourceConfigStore;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

@Component
public class ExternalVehicleSource {

    private static final ParameterizedTypeReference<ApiResponse<List<ExternalMineConfig>>> MINE_RESPONSE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<List<ExternalVehicleData>>> VEHICLE_RESPONSE =
            new ParameterizedTypeReference<>() {
    };

    private final ExternalSourceConfigStore configStore;
    private final Function<ExternalSourceConfig, RestClient> clientFactory;

    public ExternalVehicleSource(ExternalSourceConfigStore configStore) {
        this(configStore, ExternalVehicleSource::defaultClient);
    }

    ExternalVehicleSource(ExternalSourceConfigStore configStore,
                          Function<ExternalSourceConfig, RestClient> clientFactory) {
        this.configStore = configStore;
        this.clientFactory = clientFactory;
    }

    public List<ExternalMineConfig> loadMines() {
        ExternalSourceConfig config = requireEnabledConfig();
        ApiResponse<List<ExternalMineConfig>> response = client(config).get()
                .uri(config.getMineListEndpoint())
                .retrieve()
                .body(MINE_RESPONSE);
        return validData(response, "煤矿列表");
    }

    public List<ExternalVehicleData> loadVehicles(String mineCode) {
        ExternalSourceConfig config = requireEnabledConfig();
        String endpoint = config.getVehicleDataEndpoint().replace("{mineCode}",
                UriUtils.encodePathSegment(mineCode, java.nio.charset.StandardCharsets.UTF_8));
        ApiResponse<List<ExternalVehicleData>> response = client(config).get()
                .uri(endpoint)
                .retrieve()
                .body(VEHICLE_RESPONSE);
        return validData(response, "车辆数据 " + mineCode);
    }

    private ExternalSourceConfig requireEnabledConfig() {
        ExternalSourceConfig config = configStore.current();
        if (!config.isEnabled()) {
            throw new IllegalStateException("外部车辆数据源未启用");
        }
        if (!StringUtils.hasText(config.getBaseUrl())) {
            throw new IllegalStateException("外部车辆数据源地址未配置");
        }
        return config;
    }

    private RestClient client(ExternalSourceConfig config) {
        return clientFactory.apply(config);
    }

    private static RestClient defaultClient(ExternalSourceConfig config) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(config.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(config.getReadTimeoutMs()));
        return RestClient.builder()
                .baseUrl(config.getBaseUrl().trim())
                .requestFactory(requestFactory)
                .build();
    }

    private <T> List<T> validData(ApiResponse<List<T>> response, String resource) {
        if (response == null || response.getCode() != 200 || response.getData() == null) {
            throw new IllegalStateException("外部" + resource + "接口返回无效响应");
        }
        return response.getData();
    }
}
