package cn.ezios.baseapi.gcan.history.service;

import cn.ezios.baseapi.gcan.config.GcanProperties;
import cn.ezios.baseapi.gcan.history.entity.GcanCanHistory;
import cn.ezios.baseapi.gcan.history.mapper.GcanCanHistoryMapper;
import cn.ezios.baseapi.gcan.history.service.impl.CanHistoryServiceImpl;
import cn.ezios.baseapi.gcan.raw.RawCanFrame;
import cn.ezios.baseapi.gcan.vehicle.entity.GcanVehicle;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CanHistoryServiceImplTest {

    @Test
    void storeNewFramesHonorsCanIdFilterAndWatermark() {
        CapturingHistoryMapper mapper = new CapturingHistoryMapper();
        GcanProperties properties = properties(true, List.of("1836FF30"));
        CanHistoryServiceImpl service = new CanHistoryServiceImpl(mapper.proxy(), properties);
        GcanVehicle vehicle = vehicle();
        LocalDateTime receivedAt = LocalDateTime.now();

        RawCanFrame included = frame("1836FF30", receivedAt);
        RawCanFrame excluded = frame("043D9CF4", receivedAt.plusNanos(1_000_000));

        service.storeNewFrames(List.of(included, excluded), Map.of("01", vehicle));
        service.storeNewFrames(List.of(included), Map.of("01", vehicle));

        Assertions.assertEquals(1, mapper.inserted().size());
        GcanCanHistory history = mapper.inserted().getFirst();
        Assertions.assertEquals(vehicle.getId(), history.getVehicleId());
        Assertions.assertEquals("01", history.getBoxIdHex());
        Assertions.assertEquals("1836FF30", history.getCanId());
        Assertions.assertEquals(receivedAt, history.getReceivedAt());
    }

    @Test
    void storeNewFramesSkipsDisabledHistoryAndUnboundBoxes() {
        CapturingHistoryMapper mapper = new CapturingHistoryMapper();
        CanHistoryServiceImpl disabled = new CanHistoryServiceImpl(mapper.proxy(), properties(false, List.of()));
        disabled.storeNewFrames(List.of(frame("1836FF30", LocalDateTime.now())), Map.of("01", vehicle()));

        CanHistoryServiceImpl enabled = new CanHistoryServiceImpl(mapper.proxy(), properties(true, List.of()));
        enabled.storeNewFrames(List.of(frame("1836FF30", LocalDateTime.now())), Map.of());

        Assertions.assertTrue(mapper.inserted().isEmpty());
    }

    private GcanProperties properties(boolean enabled, List<String> includedCanIds) {
        GcanProperties properties = new GcanProperties();
        properties.getHistory().setEnabled(enabled);
        properties.getHistory().setIncludedCanIds(includedCanIds);
        return properties;
    }

    private GcanVehicle vehicle() {
        GcanVehicle vehicle = new GcanVehicle();
        vehicle.setId(1L);
        vehicle.setBoxIdHex("01");
        vehicle.setBoxIdDec(1);
        return vehicle;
    }

    private RawCanFrame frame(String canId, LocalDateTime receivedAt) {
        return new RawCanFrame("01", 1, canId, new int[]{1, 2, 3, 4, 5, 6, 7, 8}, receivedAt);
    }

    private static class CapturingHistoryMapper {
        private final List<GcanCanHistory> inserted = new ArrayList<>();

        GcanCanHistoryMapper proxy() {
            return (GcanCanHistoryMapper) Proxy.newProxyInstance(
                    GcanCanHistoryMapper.class.getClassLoader(),
                    new Class<?>[]{GcanCanHistoryMapper.class},
                    (proxy, method, args) -> {
                        if ("insert".equals(method.getName())) {
                            inserted.add((GcanCanHistory) args[0]);
                            return 1;
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
        }

        List<GcanCanHistory> inserted() {
            return inserted;
        }
    }
}
