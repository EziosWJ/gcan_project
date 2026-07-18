package cn.ezios.baseapi.gcan.external;

import cn.ezios.baseapi.gcan.vehicle.entity.GcanVehicle;
import cn.ezios.baseapi.gcan.state.VehicleCanState;
import cn.ezios.baseapi.gcan.state.VehicleCanStateStore;
import cn.ezios.baseapi.gcan.vehicle.service.VehicleService;
import cn.ezios.baseapi.gcan.config.ExternalSourceConfigStore;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class ExternalVehicleSyncService {

    private final ExternalVehicleSource source;
    private final VehicleService vehicleService;
    private final VehicleCanStateStore stateStore;
    private final ExternalSourceConfigStore configStore;
    private final ExternalMineNameStore mineNameStore;
    private final Map<String, Long> freshnessByMine = new ConcurrentHashMap<>();
    private static final DateTimeFormatter DATA_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ExternalVehicleSyncService(ExternalVehicleSource source, VehicleService vehicleService,
                                      VehicleCanStateStore stateStore, ExternalSourceConfigStore configStore,
                                      ExternalMineNameStore mineNameStore) {
        this.source = source;
        this.vehicleService = vehicleService;
        this.stateStore = stateStore;
        this.configStore = configStore;
        this.mineNameStore = mineNameStore;
    }

    @Transactional(rollbackFor = Exception.class)
    public void sync() {
        clearSourceError();
        List<ExternalMineConfig> mines = source.loadMines();
        for (ExternalMineConfig mine : mines) {
            if (!isEnabled(mine) || !StringUtils.hasText(mine.getMineCode())) {
                continue;
            }
            String mineCode = mine.getMineCode().trim();
            mineNameStore.put(mineCode, mine.getMineName());
            freshnessByMine.put(mineCode, freshnessMillis(mine));
            List<ExternalVehicleData> vehicles = source.loadVehicles(mineCode);
            for (ExternalVehicleData data : vehicles) {
                if (!StringUtils.hasText(data.getVehicleCode())) {
                    log.warn("外部煤矿 {} 返回缺少 vehicleCode 的车辆数据", mine.getMineCode());
                    continue;
                }
                String vehicleCode = data.getVehicleCode().trim();
                LocalDateTime receivedAt = parseDataTime(data.getDataTime());
                if (receivedAt == null) {
                    log.warn("外部车辆 {}/{} 返回缺少有效 dataTime", mineCode, vehicleCode);
                    continue;
                }
                GcanVehicle vehicle = vehicleService.findByExternalIdentity(mineCode, vehicleCode)
                        .orElseGet(() -> vehicleService.createExternal(mineCode, vehicleCode));
                stateStore.put(toState(vehicle, data, receivedAt));
            }
        }
        refreshStatuses();
    }

    public void refreshStatuses() {
        LocalDateTime now = LocalDateTime.now();
        stateStore.currentStates().stream()
                .filter(state -> "MINE_API".equals(state.getAccessMode()))
                .forEach(state -> {
                    VehicleCanState next = state;
                    LocalDateTime receivedAt = state.getLastReceivedAt();
                    if (receivedAt == null) {
                        next.setOnline(false);
                        next.setConnectionStatus("NO_DATA");
                    } else if (java.time.Duration.between(receivedAt, now).toMillis()
                            > freshnessByMine.getOrDefault(state.getMineId(), configStore.current().getPollIntervalMs()
                                    * configStore.current().getFreshnessMultiplier())) {
                        next.setOnline(false);
                        next.setConnectionStatus("OFFLINE");
                    } else {
                        next.setOnline(true);
                        next.setConnectionStatus("ONLINE");
                    }
                    next.setUpdateTime(now);
                    stateStore.put(next);
                });
    }

    public void markSourceError(String message) {
        stateStore.currentStates().stream()
                .filter(state -> "MINE_API".equals(state.getAccessMode()))
                .forEach(state -> {
                    state.setSourceError(true);
                    state.setSourceErrorMessage(message);
                    stateStore.put(state);
                });
    }

    private void clearSourceError() {
        stateStore.currentStates().stream()
                .filter(state -> "MINE_API".equals(state.getAccessMode()))
                .forEach(state -> {
                    state.setSourceError(false);
                    state.setSourceErrorMessage(null);
                    stateStore.put(state);
                });
    }

    private boolean isEnabled(ExternalMineConfig mine) {
        return mine.getEnabled() == null || mine.getEnabled() == 1;
    }

    private long freshnessMillis(ExternalMineConfig mine) {
        long frequencySeconds = mine.getPullFrequency() == null || mine.getPullFrequency() <= 0
                ? configStore.current().getPollIntervalMs() / 1000
                : mine.getPullFrequency();
        return Math.max(1000L, frequencySeconds * 1000L * configStore.current().getFreshnessMultiplier());
    }

    private VehicleCanState toState(GcanVehicle vehicle, ExternalVehicleData data, LocalDateTime receivedAt) {
        VehicleCanState state = new VehicleCanState();
        state.setVehicleId(vehicle.getId());
        state.setVehicleName(vehicle.getVehicleName());
        state.setMineId(vehicle.getMineId());
        state.setAccessMode(vehicle.getAccessMode());
        state.setExternalVehicleCode(vehicle.getExternalVehicleCode());
        state.setVehicleType(vehicle.getVehicleType());
        state.setVehicleTypeLabel(vehicle.getVehicleType());
        state.setBoxIdHex(null);
        state.setBoxIdDec(null);
        state.setOnline(true);
        state.setParseSupported(true);
        state.setParseStatus("SUPPORTED");
        state.setConnectionStatus("ONLINE");
        state.setSourceError(false);
        state.setSourceErrorMessage(null);
        state.setLastReceivedAt(receivedAt);
        state.setUpdateTime(LocalDateTime.now());
        state.setHandbrake(string(data.getHandbrakeStatus()));
        state.setInsulationState(string(data.getInsulationAlarm()));
        state.setLeftTurnLight(string(data.getLeftTurnSignal()));
        state.setRightTurnLight(string(data.getRightTurnSignal()));
        state.setHighBeam(string(data.getHighBeam()));
        state.setLowBeam(string(data.getLowBeam()));
        state.setSmallLight(string(data.getParkingLight()));
        state.setDoor1Open(string(data.getDoor1Open()));
        state.setDoor2Open(string(data.getDoor2Open()));
        state.setDoor3Open(string(data.getDoor3Open()));
        state.setMethaneDetectionFailure(string(data.getMethaneFault()));
        state.setSmokeDetectionFailure(string(data.getSmokeFault()));
        state.setRotarySpeed(data.getMotorSpeed());
        state.setMotorControllerTemperature(data.getControllerTemperature());
        state.setMotorTemperature(data.getMotorTemperature());
        state.setGear(data.getGearPosition());
        state.setReadyState(string(data.getReadyIndicator()));
        state.setLifecycle(data.getLifecycle());
        state.setThrottleOpening(decimalString(data.getAcceleratorPedalOpening()));
        state.setBrakePedalOpening(data.getBrakePedalOpening());
        state.setMotorACCurrent(data.getMotorACCurrent());
        state.setDriveActiveStatus(string(data.getDriveActiveStatus()));
        state.setBrakeActiveStatus(string(data.getBrakeActiveStatus()));
        state.setHillStartAssistStatus(string(data.getHillStartAssistStatus()));
        state.setCreepModeStatus(string(data.getCreepModeStatus()));
        state.setPrechargeContactorCmd(string(data.getPrechargeContactorCmd()));
        state.setMainContactorCmd(string(data.getMainContactorCmd()));
        state.setMotorControllerDCVoltage(data.getMotorControllerDCVoltage());
        state.setAccSignal(string(data.getAccSignal()));
        state.setOnSignal(string(data.getOnSignal()));
        state.setDriveSignal(string(data.getDriveSignal()));
        state.setReverseSignal(string(data.getReverseSignal()));
        state.setMcuTemperature(data.getMcuTemperature());
        state.setLowVoltage(data.getMinCellVoltage());
        state.setHighVoltage(data.getMaxCellVoltage());
        state.setLowTemperature(data.getMinModuleTemp());
        state.setHighTemperature(data.getMaxModuleTemp());
        state.setStartBatteryVoltage(data.getAuxiliaryBatteryVoltage());
        state.setBatteryVoltage(data.getVoltage());
        state.setBatteryElectric(data.getElectricity());
        state.setBatteryPercentage(data.getBatterySOC());
        state.setSpeed(data.getSpeed());
        state.setFaultState(string(data.getFaultCode(), "0"));
        return state;
    }

    private LocalDateTime parseDataTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), DATA_TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            log.warn("外部车辆 dataTime 无法解析: {}", value);
            return null;
        }
    }

    private String string(Number value) {
        return value == null ? null : String.valueOf(value);
    }

    private String string(Number value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private String decimalString(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }
}
