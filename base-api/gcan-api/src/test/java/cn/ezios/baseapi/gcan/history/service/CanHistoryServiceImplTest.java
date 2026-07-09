package cn.ezios.baseapi.gcan.history.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.ezios.baseapi.gcan.config.GcanProperties;
import cn.ezios.baseapi.gcan.history.entity.GcanCanHistory;
import cn.ezios.baseapi.gcan.history.mapper.GcanCanHistoryMapper;
import cn.ezios.baseapi.gcan.history.service.impl.CanHistoryServiceImpl;
import cn.ezios.baseapi.gcan.raw.RawCanFrame;
import cn.ezios.baseapi.gcan.vehicle.entity.GcanVehicle;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CanHistoryServiceImplTest {

    @Test
    void storeNewFramesHonorsCanIdFilterAndWatermark() {
        GcanCanHistoryMapper mapper = Mockito.mock(GcanCanHistoryMapper.class);
        GcanProperties properties = properties(true, List.of("1836FF30"));
        CanHistoryServiceImpl service = new CanHistoryServiceImpl(mapper, properties);
        GcanVehicle vehicle = vehicle();
        LocalDateTime receivedAt = LocalDateTime.now();

        RawCanFrame included = frame("1836FF30", receivedAt);
        RawCanFrame excluded = frame("043D9CF4", receivedAt.plusNanos(1_000_000));

        service.storeNewFrames(List.of(included, excluded), Map.of("01", vehicle));
        service.storeNewFrames(List.of(included), Map.of("01", vehicle));

        ArgumentCaptor<GcanCanHistory> captor = ArgumentCaptor.forClass(GcanCanHistory.class);
        verify(mapper, times(1)).insert(captor.capture());
        GcanCanHistory history = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(vehicle.getId(), history.getVehicleId());
        org.junit.jupiter.api.Assertions.assertEquals("01", history.getBoxIdHex());
        org.junit.jupiter.api.Assertions.assertEquals("1836FF30", history.getCanId());
        org.junit.jupiter.api.Assertions.assertEquals(receivedAt, history.getReceivedAt());
    }

    @Test
    void storeNewFramesSkipsDisabledHistoryAndUnboundBoxes() {
        GcanCanHistoryMapper mapper = Mockito.mock(GcanCanHistoryMapper.class);
        CanHistoryServiceImpl disabled = new CanHistoryServiceImpl(mapper, properties(false, List.of()));
        disabled.storeNewFrames(List.of(frame("1836FF30", LocalDateTime.now())), Map.of("01", vehicle()));

        CanHistoryServiceImpl enabled = new CanHistoryServiceImpl(mapper, properties(true, List.of()));
        enabled.storeNewFrames(List.of(frame("1836FF30", LocalDateTime.now())), Map.of());

        verify(mapper, never()).insert(any(GcanCanHistory.class));
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
}
