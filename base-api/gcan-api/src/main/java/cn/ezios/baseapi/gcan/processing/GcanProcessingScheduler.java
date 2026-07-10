package cn.ezios.baseapi.gcan.processing;

import cn.ezios.baseapi.gcan.config.GcanProperties;
import cn.ezios.baseapi.gcan.datagram.VehicleCanDatagramHandler;
import cn.ezios.baseapi.gcan.history.service.CanHistoryService;
import cn.ezios.baseapi.gcan.raw.RawCanFrame;
import cn.ezios.baseapi.gcan.raw.RawCanFrameSnapshotStore;
import cn.ezios.baseapi.gcan.state.VehicleCanState;
import cn.ezios.baseapi.gcan.state.VehicleCanStateStore;
import cn.ezios.baseapi.gcan.vehicle.entity.GcanVehicle;
import cn.ezios.baseapi.gcan.vehicle.service.VehicleService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GcanProcessingScheduler {

    private final RawCanFrameSnapshotStore rawFrameSnapshotStore;
    private final VehicleCanStateStore vehicleCanStateStore;
    private final VehicleService vehicleService;
    private final CanHistoryService canHistoryService;
    private final List<VehicleCanDatagramHandler> handlers;
    private final GcanProperties properties;

    public GcanProcessingScheduler(RawCanFrameSnapshotStore rawFrameSnapshotStore,
                                   VehicleCanStateStore vehicleCanStateStore,
                                   VehicleService vehicleService,
                                   CanHistoryService canHistoryService,
                                   List<VehicleCanDatagramHandler> handlers,
                                   GcanProperties properties) {
        this.rawFrameSnapshotStore = rawFrameSnapshotStore;
        this.vehicleCanStateStore = vehicleCanStateStore;
        this.vehicleService = vehicleService;
        this.canHistoryService = canHistoryService;
        this.handlers = handlers;
        this.properties = properties;
    }

    @Scheduled(fixedDelay = 1000)
    public void refreshVehicleCanStates() {
        Map<String, GcanVehicle> enabledVehiclesByBox = vehicleService.enabledByBoxIdHex();
        Set<Long> enabledVehicleIds = enabledVehiclesByBox.values().stream()
                .map(GcanVehicle::getId)
                .collect(Collectors.toSet());
        vehicleCanStateStore.currentStates().stream()
                .map(VehicleCanState::getVehicleId)
                .filter(vehicleId -> !enabledVehicleIds.contains(vehicleId))
                .forEach(vehicleCanStateStore::remove);
        for (GcanVehicle vehicle : enabledVehiclesByBox.values()) {
            List<RawCanFrame> frames = rawFrameSnapshotStore.currentFramesByBox(vehicle.getBoxIdHex());
            if (frames.isEmpty()) {
                continue;
            }
            LocalDateTime newest = newestReceivedAt(frames);
            if (!isFresh(newest)) {
                markOffline(vehicle, newest);
                continue;
            }
            handlers.stream()
                    .filter(handler -> handler.canHandle(vehicle.getVehicleType()))
                    .findFirst()
                    .ifPresentOrElse(handler -> {
                        VehicleCanState state = handler.handle(frames, vehicle);
                        state.setOnline(true);
                        state.setLastReceivedAt(newest);
                        vehicleCanStateStore.put(state);
                    }, () -> markUnsupported(vehicle, newest));
        }
    }

    @Scheduled(fixedDelayString = "${gcan.history.store-interval-ms:3000}")
    public void storeCanHistory() {
        Map<String, GcanVehicle> enabledVehiclesByBox = vehicleService.enabledByBoxIdHex();
        List<RawCanFrame> freshFrames = rawFrameSnapshotStore.currentFrames().stream()
                .filter(frame -> enabledVehiclesByBox.containsKey(frame.getBoxIdHex()))
                .filter(frame -> isFresh(frame.getReceivedAt()))
                .toList();
        canHistoryService.storeNewFrames(freshFrames, enabledVehiclesByBox);
    }

    private void markOffline(GcanVehicle vehicle, LocalDateTime newest) {
        VehicleCanState state = vehicleCanStateStore.get(vehicle.getId());
        if (state == null) {
            state = new VehicleCanState();
            state.setVehicleId(vehicle.getId());
            state.setVehicleName(vehicle.getVehicleName());
            state.setMineId(vehicle.getMineId());
            state.setVehicleType(vehicle.getVehicleType());
            state.setVehicleTypeLabel(vehicle.getVehicleType());
            state.setBoxIdHex(vehicle.getBoxIdHex());
            state.setBoxIdDec(vehicle.getBoxIdDec());
        }
        state.setOnline(false);
        state.setLastReceivedAt(newest);
        state.setUpdateTime(LocalDateTime.now());
        vehicleCanStateStore.put(state);
    }

    private void markUnsupported(GcanVehicle vehicle, LocalDateTime newest) {
        log.warn("未找到车辆类型 {} 的协议解析器", vehicle.getVehicleType());
        VehicleCanState state = new VehicleCanState();
        state.setVehicleId(vehicle.getId());
        state.setVehicleName(vehicle.getVehicleName());
        state.setMineId(vehicle.getMineId());
        state.setVehicleType(vehicle.getVehicleType());
        state.setVehicleTypeLabel(vehicle.getVehicleType());
        state.setBoxIdHex(vehicle.getBoxIdHex());
        state.setBoxIdDec(vehicle.getBoxIdDec());
        state.setOnline(true);
        state.setParseSupported(false);
        state.setParseMessage("未支持解析");
        state.setLastReceivedAt(newest);
        state.setUpdateTime(LocalDateTime.now());
        vehicleCanStateStore.put(state);
    }

    private boolean isFresh(LocalDateTime time) {
        if (time == null) {
            return false;
        }
        return Duration.between(time, LocalDateTime.now()).toMillis() <= properties.getFrameStaleThresholdMs();
    }

    private LocalDateTime newestReceivedAt(List<RawCanFrame> frames) {
        return frames.stream()
                .map(RawCanFrame::getReceivedAt)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }
}
