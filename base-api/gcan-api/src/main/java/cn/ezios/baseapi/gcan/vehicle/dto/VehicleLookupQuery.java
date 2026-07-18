package cn.ezios.baseapi.gcan.vehicle.dto;

import lombok.Data;

@Data
public class VehicleLookupQuery {

    private String vehicleName;
    private String mineId;
    private String vehicleType;
    private String boxIdHex;
    private String externalVehicleCode;
    private String accessMode;
}
