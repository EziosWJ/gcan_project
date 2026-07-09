package cn.ezios.baseapi.gcan.datagram;

import cn.ezios.baseapi.gcan.vehicle.VehicleType;
import org.springframework.stereotype.Component;

@Component
public class Material19TDatagramHandler extends AbstractMaterialVehicleDatagramHandler {

    @Override
    public boolean canHandle(String vehicleType) {
        return VehicleType.LIAO_1_9T.name().equals(vehicleType);
    }
}
