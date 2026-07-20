package cn.ezios.baseapi.gcan.raw.mirror;

import cn.ezios.baseapi.common.model.ApiResponse;
import cn.ezios.baseapi.gcan.common.BoxIdUtil;
import cn.ezios.baseapi.gcan.config.GcanProperties;
import cn.ezios.baseapi.gcan.raw.RawCanFrame;
import cn.ezios.baseapi.gcan.raw.RawCanFrameSource;
import cn.ezios.baseapi.gcan.raw.vo.RawCanFrameVO;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class HttpGcanRawFrameSource implements GcanRawFrameSource {

    private static final ParameterizedTypeReference<ApiResponse<List<RawCanFrameVO>>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final GcanProperties.Mirror properties;

    @Autowired
    public HttpGcanRawFrameSource(GcanProperties properties) {
        this(properties.getMirror(), createRestClient(properties.getMirror()));
    }

    HttpGcanRawFrameSource(GcanProperties.Mirror properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public List<RawCanFrame> load(String boxIdHex) {
        String normalizedBoxId = BoxIdUtil.normalizeHex(boxIdHex);
        ApiResponse<List<RawCanFrameVO>> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(properties.getEndpoint())
                        .queryParam("boxIdHex", normalizedBoxId)
                        .queryParam("format", "DECIMAL")
                        .build())
                .retrieve()
                .body(RESPONSE_TYPE);
        if (response == null || response.getCode() != 200 || response.getData() == null) {
            throw new IllegalStateException("GCAN 原始 CAN 镜像接口返回无效响应");
        }
        return response.getData().stream()
                .map(frame -> toRawCanFrame(frame, normalizedBoxId))
                .toList();
    }

    private RawCanFrameVO requireFrame(RawCanFrameVO frame) {
        if (frame == null) {
            throw new IllegalStateException("GCAN 原始 CAN 镜像接口返回空帧");
        }
        return frame;
    }

    private RawCanFrame toRawCanFrame(RawCanFrameVO source, String requestedBoxId) {
        RawCanFrameVO frame = requireFrame(source);
        if (!StringUtils.hasText(frame.getBoxIdHex())) {
            throw new IllegalStateException("GCAN 原始 CAN 镜像帧缺少盒子 ID");
        }
        String boxIdHex = BoxIdUtil.normalizeHex(frame.getBoxIdHex());
        if (!requestedBoxId.equals(boxIdHex)) {
            throw new IllegalStateException("GCAN 原始 CAN 镜像帧盒子 ID 与请求不一致");
        }
        if (!StringUtils.hasText(frame.getCanId()) || frame.getReceivedAt() == null) {
            throw new IllegalStateException("GCAN 原始 CAN 镜像帧缺少 CAN ID 或接收时间");
        }
        if (frame.getData() == null || frame.getData().size() != 8) {
            throw new IllegalStateException("GCAN 原始 CAN 镜像帧必须包含 8 个十进制字节");
        }

        int[] values = new int[8];
        for (int i = 0; i < values.length; i++) {
            values[i] = parseByte(frame.getData().get(i));
        }
        return new RawCanFrame(
                boxIdHex,
                BoxIdUtil.toDec(boxIdHex),
                frame.getCanId().trim().toUpperCase(),
                values,
                frame.getReceivedAt(),
                RawCanFrameSource.MIRROR);
    }

    private int parseByte(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("GCAN 原始 CAN 镜像帧包含空字节");
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 0 || parsed > 255) {
                throw new IllegalStateException("GCAN 原始 CAN 镜像帧字节超出 0-255 范围");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("GCAN 原始 CAN 镜像帧包含非十进制字节", exception);
        }
    }

    private static RestClient createRestClient(GcanProperties.Mirror properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        RestClient.Builder builder = RestClient.builder().requestFactory(requestFactory);
        if (StringUtils.hasText(properties.getBaseUrl())) {
            builder.baseUrl(properties.getBaseUrl());
        }
        return builder.build();
    }
}
