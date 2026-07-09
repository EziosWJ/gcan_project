package cn.ezios.baseapi.gcan.history.service;

import cn.ezios.baseapi.gcan.raw.RawCanFrame;
import cn.ezios.baseapi.gcan.vehicle.entity.GcanVehicle;
import java.util.Collection;
import java.util.Map;

public interface CanHistoryService {

    void storeNewFrames(Collection<RawCanFrame> frames, Map<String, GcanVehicle> enabledVehiclesByBox);
}
