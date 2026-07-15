package cn.ezios.baseapi.gcan.dictionary;

import cn.ezios.baseapi.common.model.ApiResponse;
import cn.ezios.baseapi.gcan.config.GcanProperties;
import java.time.Duration;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpGcanDictionarySource implements GcanDictionarySource {

    private static final ParameterizedTypeReference<ApiResponse<List<GcanDictionaryGroup>>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final GcanProperties.Dictionary properties;

    public HttpGcanDictionarySource(GcanProperties properties) {
        this.properties = properties.getDictionary();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(this.properties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(this.properties.getReadTimeoutMs()));
        this.restClient = RestClient.builder()
                .baseUrl(this.properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public List<GcanDictionaryGroup> load() {
        ApiResponse<List<GcanDictionaryGroup>> response = restClient.get()
                .uri(properties.getEndpoint())
                .retrieve()
                .body(RESPONSE_TYPE);
        if (response == null || response.getCode() != 200 || response.getData() == null) {
            throw new IllegalStateException("GCAN字典契约返回无效响应");
        }
        return response.getData();
    }
}
