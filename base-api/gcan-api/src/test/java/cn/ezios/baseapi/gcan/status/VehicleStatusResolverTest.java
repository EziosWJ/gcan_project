package cn.ezios.baseapi.gcan.status;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.ezios.baseapi.gcan.config.GcanProperties;
import cn.ezios.baseapi.gcan.datagram.Material19TDatagramHandler;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class VehicleStatusResolverTest {

    @Test
    void connectionAndParseStatusesAreResolvedIndependently() {
        GcanProperties properties = new GcanProperties();
        properties.setFrameStaleThresholdMs(10_000L);
        VehicleStatusResolver resolver = new VehicleStatusResolver(
                List.of(new Material19TDatagramHandler()), properties);

        assertEquals(VehicleConnectionStatus.NO_DATA,
                resolver.resolve("FUTURE_TYPE", null).connectionStatus());
        assertEquals(VehicleParseStatus.UNSUPPORTED,
                resolver.resolve("FUTURE_TYPE", LocalDateTime.now()).parseStatus());
        assertEquals(VehicleConnectionStatus.OFFLINE,
                resolver.resolve("LIAO_1_9T", LocalDateTime.now().minusSeconds(20)).connectionStatus());
        assertEquals(VehicleParseStatus.SUPPORTED,
                resolver.resolve("LIAO_1_9T", null).parseStatus());
    }
}
