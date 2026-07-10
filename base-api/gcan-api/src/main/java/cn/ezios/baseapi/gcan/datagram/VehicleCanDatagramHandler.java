package cn.ezios.baseapi.gcan.datagram;

import cn.ezios.baseapi.gcan.raw.RawCanFrame;
import cn.ezios.baseapi.gcan.state.VehicleCanState;
import cn.ezios.baseapi.gcan.vehicle.entity.GcanVehicle;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public interface VehicleCanDatagramHandler {

    Set<String> supportedVehicleTypes();

    default boolean canHandle(String vehicleType) {
        if (vehicleType == null) {
            return false;
        }
        return supportedVehicleTypes().contains(vehicleType.trim().toUpperCase(Locale.ROOT));
    }

    VehicleCanState handle(List<RawCanFrame> frames, GcanVehicle vehicle);
}
