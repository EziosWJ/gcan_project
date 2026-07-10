package cn.ezios.baseapi.gcan.state.dto;

import cn.ezios.baseapi.gcan.vehicle.dto.VehicleLookupQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class VehicleCanStateQuery extends VehicleLookupQuery {

    private Boolean online;
}
