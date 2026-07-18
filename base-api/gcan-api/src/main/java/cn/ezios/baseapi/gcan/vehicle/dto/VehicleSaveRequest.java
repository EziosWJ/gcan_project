package cn.ezios.baseapi.gcan.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VehicleSaveRequest {

    @NotBlank(message = "车辆名称不能为空")
    @Size(max = 100, message = "车辆名称长度不能超过 100")
    private String vehicleName;

    @NotBlank(message = "煤矿不能为空")
    @Size(max = 100, message = "煤矿ID长度不能超过 100")
    private String mineId;

    @Size(max = 20, message = "接入方式长度不能超过 20")
    private String accessMode;

    @Size(max = 100, message = "外部车辆编码长度不能超过 100")
    private String externalVehicleCode;

    @NotBlank(message = "车辆类型不能为空")
    @Size(max = 50, message = "车辆类型长度不能超过 50")
    private String vehicleType;

    @Size(max = 100, message = "故障码表编码长度不能超过 100")
    private String faultProfileCode;

    private String boxIdHex;

    private Integer status;

    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}
