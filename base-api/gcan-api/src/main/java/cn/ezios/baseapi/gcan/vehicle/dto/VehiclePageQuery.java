package cn.ezios.baseapi.gcan.vehicle.dto;

import cn.ezios.baseapi.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class VehiclePageQuery extends PageQuery {

    private String vehicleName;
    private String mineId;
    private String accessMode;
    private String vehicleType;
    private String boxIdHex;
    private Integer status;
}
