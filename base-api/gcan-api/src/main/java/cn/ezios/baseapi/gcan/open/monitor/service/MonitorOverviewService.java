package cn.ezios.baseapi.gcan.open.monitor.service;

import cn.ezios.baseapi.gcan.dictionary.GcanDictionaryCodes;
import cn.ezios.baseapi.gcan.dictionary.GcanDictionaryNameService;
import cn.ezios.baseapi.gcan.fault.service.FaultProfileService;
import cn.ezios.baseapi.gcan.fault.vo.FaultResultVO;
import cn.ezios.baseapi.gcan.open.monitor.vo.FaultVO;
import cn.ezios.baseapi.gcan.open.monitor.vo.MonitorMineVO;
import cn.ezios.baseapi.gcan.open.monitor.vo.MonitorOverviewVO;
import cn.ezios.baseapi.gcan.open.monitor.vo.MonitorStatsVO;
import cn.ezios.baseapi.gcan.open.monitor.vo.PublicVehicleStateVO;
import cn.ezios.baseapi.gcan.state.VehicleCanState;
import cn.ezios.baseapi.gcan.state.VehicleCanStateStore;
import cn.ezios.baseapi.gcan.vehicle.entity.GcanVehicle;
import cn.ezios.baseapi.gcan.vehicle.service.VehicleService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class MonitorOverviewService {

    private final VehicleCanStateStore stateStore;
    private final VehicleService vehicleService;
    private final GcanDictionaryNameService dictionaryNameService;
    private final FaultProfileService faultProfileService;

    public MonitorOverviewService(VehicleCanStateStore stateStore,
                                  VehicleService vehicleService,
                                  GcanDictionaryNameService dictionaryNameService,
                                  FaultProfileService faultProfileService) {
        this.stateStore = stateStore;
        this.vehicleService = vehicleService;
        this.dictionaryNameService = dictionaryNameService;
        this.faultProfileService = faultProfileService;
    }

    public MonitorOverviewVO overview() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, List<PublicVehicleStateVO>> grouped = new LinkedHashMap<>();
        for (GcanVehicle vehicle : vehicleService.enabledByBoxIdHex().values().stream()
                .sorted(Comparator.comparing(GcanVehicle::getMineId, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(GcanVehicle::getVehicleName, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(GcanVehicle::getId))
                .toList()) {
            VehicleCanState state = stateStore.get(vehicle.getId());
            PublicVehicleStateVO publicState = toPublicState(vehicle, state);
            grouped.computeIfAbsent(vehicle.getMineId(), ignored -> new ArrayList<>()).add(publicState);
        }

        MonitorOverviewVO result = new MonitorOverviewVO();
        result.setGeneratedAt(now);
        result.setLastUpdateAt(grouped.values().stream().flatMap(List::stream)
                .map(PublicVehicleStateVO::getUpdateTime)
                .filter(value -> value != null)
                .max(Comparator.naturalOrder())
                .orElse(null));
        result.setMines(grouped.entrySet().stream().map(entry -> {
            MonitorMineVO mine = new MonitorMineVO();
            mine.setMineId(entry.getKey());
            mine.setMineName(name(GcanDictionaryCodes.MINE, entry.getKey()));
            mine.setVehicles(entry.getValue());
            mine.setStatistics(stats(entry.getValue()));
            return mine;
        }).toList());
        result.setStatistics(stats(result.getMines().stream().flatMap(mine -> mine.getVehicles().stream()).toList()));
        return result;
    }

    private PublicVehicleStateVO toPublicState(GcanVehicle vehicle, VehicleCanState state) {
        PublicVehicleStateVO result = new PublicVehicleStateVO();
        if (state != null) {
            BeanUtils.copyProperties(state, result);
        } else {
            result.setVehicleId(vehicle.getId());
            result.setVehicleName(vehicle.getVehicleName());
            result.setMineId(vehicle.getMineId());
            result.setVehicleType(vehicle.getVehicleType());
            result.setVehicleTypeLabel(vehicle.getVehicleType());
            result.setBoxIdHex(vehicle.getBoxIdHex());
            result.setBoxIdDec(vehicle.getBoxIdDec());
            result.setConnectionStatus("NO_DATA");
            result.setParseStatus("SUPPORTED");
            result.setParseSupported(true);
            result.setParseMessage("暂无数据");
        }
        result.setMineName(name(GcanDictionaryCodes.MINE, vehicle.getMineId()));
        result.setVehicleTypeLabel(name(GcanDictionaryCodes.VEHICLE_TYPE, vehicle.getVehicleType()));
        result.setConnectionStatusLabel(name(GcanDictionaryCodes.VEHICLE_CONNECTION_STATUS,
                result.getConnectionStatus()));
        result.setParseStatusLabel(name(GcanDictionaryCodes.VEHICLE_PARSE_STATUS, result.getParseStatus()));
        result.setFault(toFault(vehicle, result));
        result.setSupportedUnits(List.of("V", "A", "℃", "%", "km/h", "rpm", "km"));
        return result;
    }

    private FaultVO toFault(GcanVehicle vehicle, PublicVehicleStateVO state) {
        String code = state.getFaultState();
        if (code == null || code.isBlank() || "NO_DATA".equals(state.getConnectionStatus())
                || "UNSUPPORTED".equals(state.getParseStatus())) {
            return null;
        }
        FaultVO result = new FaultVO();
        result.setCode(code);
        result.setActive(!"0".equals(code));
        result.setStale("OFFLINE".equals(state.getConnectionStatus()));
        if (faultProfileService == null) {
            result.setConfigured(false);
            result.setMatched(false);
            return result;
        }
        FaultResultVO resolved = faultProfileService.resolve(vehicle.getFaultProfileCode(), code);
        result.setConfigured(!FaultResultVO.UNCONFIGURED_PROFILE.equals(resolved.getStatus()));
        result.setMatched(FaultResultVO.MATCHED.equals(resolved.getStatus()));
        result.setLevelCode(resolved.getRawLevelCode());
        result.setLevelName(resolved.getRawLevelName());
        result.setName(resolved.getFaultName());
        result.setDefinition(resolved.getFaultDefinition());
        result.setDescription(resolved.getFaultDefinition());
        result.setAnalysis(resolved.getAnalysis());
        result.setSymptom(resolved.getSymptom());
        result.setRecovery(resolved.getRecovery());
        result.setClear(resolved.getRemoval());
        result.setHandlingAdvice(resolved.getHandlingSuggestion());
        result.setSuggestion(resolved.getHandlingSuggestion());
        return result;
    }

    private MonitorStatsVO stats(List<PublicVehicleStateVO> vehicles) {
        MonitorStatsVO stats = new MonitorStatsVO();
        stats.setVehicleTotal(vehicles.size());
        stats.setOnlineCount(vehicles.stream().filter(item -> "ONLINE".equals(item.getConnectionStatus())).count());
        stats.setOfflineCount(vehicles.stream().filter(item -> "OFFLINE".equals(item.getConnectionStatus())).count());
        stats.setNoDataCount(vehicles.stream().filter(item -> "NO_DATA".equals(item.getConnectionStatus())).count());
        stats.setUnsupportedCount(vehicles.stream().filter(item -> "UNSUPPORTED".equals(item.getParseStatus())).count());
        stats.setFaultVehicleCount(vehicles.stream()
                .filter(item -> "ONLINE".equals(item.getConnectionStatus()))
                .filter(item -> "SUPPORTED".equals(item.getParseStatus()))
                .filter(item -> item.getFault() != null && item.getFault().isActive())
                .count());
        stats.setLatestDataAt(vehicles.stream().map(PublicVehicleStateVO::getLastReceivedAt)
                .filter(value -> value != null).max(Comparator.naturalOrder()).orElse(null));
        return stats;
    }

    private String name(String dictCode, String code) {
        return dictionaryNameService == null ? code : dictionaryNameService.name(dictCode, code);
    }
}
