package cn.ezios.baseapi.gcan.vehicle.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class VehicleVO {

    private Long id;
    private String vehicleName;
    private String mineId;
    private String vehicleType;
    private String vehicleTypeLabel;
    private String faultProfileCode;
    private String boxIdHex;
    private Integer boxIdDec;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
