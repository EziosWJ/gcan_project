package cn.ezios.baseapi.gcan.raw.dto;

import cn.ezios.baseapi.common.model.PageQuery;
import cn.ezios.baseapi.gcan.vehicle.dto.VehicleLookupQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RawCanFrameQuery extends PageQuery {

    private String vehicleName;
    private String mineId;
    private String vehicleType;
    private String boxIdHex;
    private String canId;

    public VehicleLookupQuery toVehicleLookupQuery() {
        VehicleLookupQuery query = new VehicleLookupQuery();
        query.setVehicleName(vehicleName);
        query.setMineId(mineId);
        query.setVehicleType(vehicleType);
        query.setBoxIdHex(boxIdHex);
        return query;
    }
}
