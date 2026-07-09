package cn.ezios.baseapi.gcan.datagram;

import cn.ezios.baseapi.gcan.raw.RawCanFrame;
import cn.ezios.baseapi.gcan.state.VehicleCanState;
import cn.ezios.baseapi.gcan.vehicle.VehicleType;
import cn.ezios.baseapi.gcan.vehicle.entity.GcanVehicle;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractVehicleCanDatagramHandler implements VehicleCanDatagramHandler {

    protected VehicleCanState baseState(GcanVehicle vehicle) {
        VehicleCanState state = new VehicleCanState();
        state.setVehicleId(vehicle.getId());
        state.setVehicleName(vehicle.getVehicleName());
        state.setVehicleType(vehicle.getVehicleType());
        state.setVehicleTypeLabel(VehicleType.valueOf(vehicle.getVehicleType()).getLabel());
        state.setBoxIdHex(vehicle.getBoxIdHex());
        state.setBoxIdDec(vehicle.getBoxIdDec());
        state.setUpdateTime(LocalDateTime.now());
        return state;
    }

    protected Map<String, RawCanFrame> byCanId(List<RawCanFrame> frames) {
        Map<String, RawCanFrame> map = new HashMap<>();
        for (RawCanFrame frame : frames) {
            map.put(frame.getCanId(), frame);
        }
        return map;
    }

    protected BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }

    protected int wordLH(RawCanFrame frame, int lowIndex, int highIndex) {
        return frame.getValue(lowIndex) + frame.getValue(highIndex) * 256;
    }

    protected int wordHL(RawCanFrame frame, int highIndex, int lowIndex) {
        return frame.getValue(highIndex) * 256 + frame.getValue(lowIndex);
    }
}
