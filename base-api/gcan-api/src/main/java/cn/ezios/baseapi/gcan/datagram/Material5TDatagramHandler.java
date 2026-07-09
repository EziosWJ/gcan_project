package cn.ezios.baseapi.gcan.datagram;

import cn.ezios.baseapi.gcan.vehicle.VehicleType;
import org.springframework.stereotype.Component;

@Component
public class Material5TDatagramHandler extends AbstractMaterialVehicleDatagramHandler {

    @Override
    public boolean canHandle(String vehicleType) {
        return VehicleType.LIAO_5T.name().equals(vehicleType);
    }
}
