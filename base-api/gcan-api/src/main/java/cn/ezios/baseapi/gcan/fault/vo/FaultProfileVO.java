package cn.ezios.baseapi.gcan.fault.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FaultProfileVO {

    private Long id;
    private String profileCode;
    private String profileName;
    private String manufacturer;
    private String vehicleType;
    private String protocolVersion;
    private String applicableVehicleDescription;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
