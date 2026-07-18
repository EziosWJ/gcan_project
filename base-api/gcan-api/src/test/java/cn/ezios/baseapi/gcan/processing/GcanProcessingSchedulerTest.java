package cn.ezios.baseapi.gcan.processing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.ezios.baseapi.common.model.BatchIdsRequest;
import cn.ezios.baseapi.common.model.PageResult;
import cn.ezios.baseapi.common.model.StatusUpdateRequest;
import cn.ezios.baseapi.gcan.config.GcanProperties;
import cn.ezios.baseapi.gcan.datagram.Material19TDatagramHandler;
import cn.ezios.baseapi.gcan.history.service.CanHistoryService;
import cn.ezios.baseapi.gcan.raw.RawCanFrame;
import cn.ezios.baseapi.gcan.raw.RawCanFrameSnapshotStore;
import cn.ezios.baseapi.gcan.state.VehicleCanStateStore;
import cn.ezios.baseapi.gcan.vehicle.dto.VehiclePageQuery;
import cn.ezios.baseapi.gcan.vehicle.dto.VehicleLookupQuery;
import cn.ezios.baseapi.gcan.vehicle.dto.VehicleSaveRequest;
import cn.ezios.baseapi.gcan.vehicle.entity.GcanVehicle;
import cn.ezios.baseapi.gcan.vehicle.service.VehicleService;
import cn.ezios.baseapi.gcan.vehicle.vo.VehicleVO;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GcanProcessingSchedulerTest {

    @Test
    void refreshVehicleCanStatesRemovesStateWhenVehicleIsDisabled() {
        RawCanFrameSnapshotStore rawStore = new RawCanFrameSnapshotStore();
        VehicleCanStateStore stateStore = new VehicleCanStateStore();
        MutableVehicleService vehicleService = new MutableVehicleService(vehicle());
        GcanProcessingScheduler scheduler = scheduler(rawStore, stateStore, vehicleService);

        rawStore.put(frame(LocalDateTime.now()));
        scheduler.refreshVehicleCanStates();
        assertTrue(stateStore.get(1L).getOnline());

        vehicleService.vehicle = null;
        scheduler.refreshVehicleCanStates();
        assertNull(stateStore.get(1L));
    }

    @Test
    void refreshVehicleCanStatesMarksExistingStateOfflineWhenFramesAreStale() {
        RawCanFrameSnapshotStore rawStore = new RawCanFrameSnapshotStore();
        VehicleCanStateStore stateStore = new VehicleCanStateStore();
        MutableVehicleService vehicleService = new MutableVehicleService(vehicle());
        GcanProcessingScheduler scheduler = scheduler(rawStore, stateStore, vehicleService);

        rawStore.put(frame(LocalDateTime.now()));
        scheduler.refreshVehicleCanStates();

        rawStore.put(frame(LocalDateTime.now().minusSeconds(20)));
        scheduler.refreshVehicleCanStates();

        assertFalse(stateStore.get(1L).getOnline());
    }

    @Test
    void refreshVehicleCanStatesMarksUnsupportedParserWhenVehicleTypeHasNoHandler() {
        RawCanFrameSnapshotStore rawStore = new RawCanFrameSnapshotStore();
        VehicleCanStateStore stateStore = new VehicleCanStateStore();
        GcanVehicle vehicle = vehicle();
        vehicle.setVehicleType("FUTURE_TYPE");
        MutableVehicleService vehicleService = new MutableVehicleService(vehicle);
        GcanProcessingScheduler scheduler = scheduler(rawStore, stateStore, vehicleService);

        rawStore.put(frame(LocalDateTime.now()));
        scheduler.refreshVehicleCanStates();

        assertTrue(stateStore.get(1L).getOnline());
        assertFalse(stateStore.get(1L).getParseSupported());
    }

    @Test
    void refreshVehicleCanStatesCreatesNoDataStateForEnabledVehicleWithoutFrames() {
        RawCanFrameSnapshotStore rawStore = new RawCanFrameSnapshotStore();
        VehicleCanStateStore stateStore = new VehicleCanStateStore();
        MutableVehicleService vehicleService = new MutableVehicleService(vehicle());
        GcanProcessingScheduler scheduler = scheduler(rawStore, stateStore, vehicleService);

        scheduler.refreshVehicleCanStates();

        assertEquals("NO_DATA", stateStore.get(1L).getConnectionStatus());
        assertEquals("SUPPORTED", stateStore.get(1L).getParseStatus());
        assertEquals("暂无数据", stateStore.get(1L).getParseMessage());
    }

    private GcanProcessingScheduler scheduler(RawCanFrameSnapshotStore rawStore,
                                              VehicleCanStateStore stateStore,
                                              VehicleService vehicleService) {
        GcanProperties properties = new GcanProperties();
        properties.setFrameStaleThresholdMs(10000);
        return new GcanProcessingScheduler(
                rawStore,
                stateStore,
                vehicleService,
                new NoopHistoryService(),
                List.of(new Material19TDatagramHandler()),
                properties);
    }

    private GcanVehicle vehicle() {
        GcanVehicle vehicle = new GcanVehicle();
        vehicle.setId(1L);
        vehicle.setVehicleName("测试车辆");
        vehicle.setMineId("MINE_TEST");
        vehicle.setVehicleType("LIAO_1_9T");
        vehicle.setBoxIdHex("01");
        vehicle.setBoxIdDec(1);
        vehicle.setStatus(1);
        return vehicle;
    }

    private RawCanFrame frame(LocalDateTime receivedAt) {
        return new RawCanFrame("01", 1, "08F200A0", new int[]{0, 0, 0, 0, 0, 60, 0, 0}, receivedAt);
    }

    private static class NoopHistoryService implements CanHistoryService {
        @Override
        public void storeNewFrames(Collection<RawCanFrame> frames, Map<String, GcanVehicle> enabledVehiclesByBox) {
        }
    }

    private static class MutableVehicleService implements VehicleService {
        private GcanVehicle vehicle;

        MutableVehicleService(GcanVehicle vehicle) {
            this.vehicle = vehicle;
        }

        @Override
        public Map<String, GcanVehicle> enabledByBoxIdHex() {
            return vehicle == null ? Map.of() : Map.of(vehicle.getBoxIdHex(), vehicle);
        }

        @Override
        public Map<String, GcanVehicle> enabledByBoxIdHex(VehicleLookupQuery query) {
            return enabledByBoxIdHex();
        }

        @Override
        public Map<String, GcanVehicle> byBoxIdHex(VehicleLookupQuery query) {
            return enabledByBoxIdHex();
        }

        @Override
        public List<GcanVehicle> enabledVehicles(VehicleLookupQuery query) {
            return vehicle == null ? List.of() : List.of(vehicle);
        }

        @Override
        public Optional<GcanVehicle> findByExternalIdentity(String mineId, String externalVehicleCode) {
            return Optional.empty();
        }

        @Override
        public GcanVehicle createExternal(String mineId, String externalVehicleCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<VehicleVO> page(VehiclePageQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<VehicleVO> listEnabled() {
            throw new UnsupportedOperationException();
        }

        @Override
        public VehicleVO getDetail(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void create(VehicleSaveRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void update(Long id, VehicleSaveRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteBatch(BatchIdsRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateStatus(Long id, StatusUpdateRequest request) {
            throw new UnsupportedOperationException();
        }
    }
}
