package cn.ezios.baseapi.gcan.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VehicleSaveRequest {

    @NotBlank(message = "车辆名称不能为空")
    @Size(max = 100, message = "车辆名称长度不能超过 100")
    private String vehicleName;

    @NotBlank(message = "车辆类型不能为空")
    private String vehicleType;

    @NotBlank(message = "盒子ID(HEX)不能为空")
    private String boxIdHex;

    private Integer status;

    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}
