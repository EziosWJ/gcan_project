package cn.ezios.baseapi.gcan.datagram;

import cn.ezios.baseapi.gcan.vehicle.VehicleType;
import org.springframework.stereotype.Component;

@Component
public class Passenger19DatagramHandler extends AbstractPassengerVehicleDatagramHandler {

    @Override
    public boolean canHandle(String vehicleType) {
        return VehicleType.REN_19.name().equals(vehicleType);
    }
}
