package cn.ezios.baseapi.gcan.raw.mirror;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.ezios.baseapi.gcan.config.GcanProperties;
import cn.ezios.baseapi.gcan.raw.RawCanFrame;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpGcanRawFrameSourceTest {

    @Test
    void loadsDecimalFramesFromOpenApiAndPreservesReceivedAt() {
        GcanProperties.Mirror properties = new GcanProperties.Mirror();
        properties.setBaseUrl("http://mirror.test");
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpGcanRawFrameSource source = new HttpGcanRawFrameSource(properties, builder.build());
        LocalDateTime receivedAt = LocalDateTime.of(2026, 7, 16, 14, 30, 0);

        server.expect(requestTo("http://mirror.test/api/open/gcan/v1/raw-frame/current?boxIdHex=01&format=DECIMAL"))
                .andRespond(withSuccess("""
                        {
                          "code": 200,
                          "message": "success",
                          "data": [{
                            "boxIdHex": "01",
                            "boxIdDec": 1,
                            "canId": "a001",
                            "data": ["0", "1", "2", "3", "4", "5", "254", "255"],
                            "receivedAt": "2026-07-16T14:30:00"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<RawCanFrame> frames = source.load("0x1");

        assertEquals(1, frames.size());
        assertEquals("01", frames.getFirst().getBoxIdHex());
        assertEquals("A001", frames.getFirst().getCanId());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 254, 255),
                java.util.Arrays.stream(frames.getFirst().values()).boxed().toList());
        assertEquals(receivedAt, frames.getFirst().getReceivedAt());
        server.verify();
    }
}
