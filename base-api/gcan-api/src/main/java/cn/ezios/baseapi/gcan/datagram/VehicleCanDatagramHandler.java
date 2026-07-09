package cn.ezios.baseapi.gcan.datagram;

import cn.ezios.baseapi.gcan.raw.RawCanFrame;
import cn.ezios.baseapi.gcan.state.VehicleCanState;
import cn.ezios.baseapi.gcan.vehicle.entity.GcanVehicle;
import java.util.List;

public interface VehicleCanDatagramHandler {

    boolean canHandle(String vehicleType);

    VehicleCanState handle(List<RawCanFrame> frames, GcanVehicle vehicle);
}
