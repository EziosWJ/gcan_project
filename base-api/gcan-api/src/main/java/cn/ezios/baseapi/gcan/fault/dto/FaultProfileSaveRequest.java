package cn.ezios.baseapi.gcan.fault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FaultProfileSaveRequest {

    @NotBlank(message = "故障码表编码不能为空")
    @Size(max = 100, message = "故障码表编码长度不能超过 100")
    private String profileCode;

    @NotBlank(message = "故障码表名称不能为空")
    @Size(max = 100, message = "故障码表名称长度不能超过 100")
    private String profileName;

    @Size(max = 100, message = "厂家长度不能超过 100")
    private String manufacturer;

    @Size(max = 50, message = "适用车型编码长度不能超过 50")
    private String vehicleType;

    @Size(max = 100, message = "协议版本长度不能超过 100")
    private String protocolVersion;

    @Size(max = 500, message = "适用车型说明长度不能超过 500")
    private String applicableVehicleDescription;

    private Integer status;

    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}
