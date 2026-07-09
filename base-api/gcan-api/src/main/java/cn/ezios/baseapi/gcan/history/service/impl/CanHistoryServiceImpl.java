package cn.ezios.baseapi.gcan.history.service.impl;

import cn.ezios.baseapi.gcan.config.GcanProperties;
import cn.ezios.baseapi.gcan.history.entity.GcanCanHistory;
import cn.ezios.baseapi.gcan.history.mapper.GcanCanHistoryMapper;
import cn.ezios.baseapi.gcan.history.service.CanHistoryService;
import cn.ezios.baseapi.gcan.raw.RawCanFrame;
import cn.ezios.baseapi.gcan.vehicle.entity.GcanVehicle;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CanHistoryServiceImpl implements CanHistoryService {

    private final GcanCanHistoryMapper historyMapper;
    private final GcanProperties properties;
    private final Map<String, LocalDateTime> writeWatermarks = new ConcurrentHashMap<>();

    public CanHistoryServiceImpl(GcanCanHistoryMapper historyMapper, GcanProperties properties) {
        this.historyMapper = historyMapper;
        this.properties = properties;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void storeNewFrames(Collection<RawCanFrame> frames, Map<String, GcanVehicle> enabledVehiclesByBox) {
        if (!properties.getHistory().isEnabled()) {
            return;
        }
        Set<String> includedCanIds = normalizedIncludedCanIds();
        for (RawCanFrame frame : frames) {
            GcanVehicle vehicle = enabledVehiclesByBox.get(frame.getBoxIdHex());
            if (vehicle == null) {
                continue;
            }
            if (!includedCanIds.isEmpty() && !includedCanIds.contains(frame.getCanId())) {
                continue;
            }
            String key = frame.getBoxIdHex() + ":" + frame.getCanId();
            LocalDateTime watermark = writeWatermarks.get(key);
            if (watermark != null && !frame.getReceivedAt().isAfter(watermark)) {
                continue;
            }
            historyMapper.insert(toHistory(frame, vehicle));
            writeWatermarks.put(key, frame.getReceivedAt());
        }
    }

    private Set<String> normalizedIncludedCanIds() {
        Set<String> values = new HashSet<>();
        for (String canId : properties.getHistory().getIncludedCanIds()) {
            if (canId != null && !canId.isBlank()) {
                values.add(canId.trim().toUpperCase());
            }
        }
        return values;
    }

    private GcanCanHistory toHistory(RawCanFrame frame, GcanVehicle vehicle) {
        int[] values = frame.values();
        GcanCanHistory history = new GcanCanHistory();
        history.setVehicleId(vehicle.getId());
        history.setBoxIdHex(frame.getBoxIdHex());
        history.setBoxIdDec(frame.getBoxIdDec());
        history.setCanId(frame.getCanId());
        history.setValue0(values[0]);
        history.setValue1(values[1]);
        history.setValue2(values[2]);
        history.setValue3(values[3]);
        history.setValue4(values[4]);
        history.setValue5(values[5]);
        history.setValue6(values[6]);
        history.setValue7(values[7]);
        history.setReceivedAt(frame.getReceivedAt());
        return history;
    }
}
