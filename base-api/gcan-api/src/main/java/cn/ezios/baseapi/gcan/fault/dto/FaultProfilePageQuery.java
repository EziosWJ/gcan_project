package cn.ezios.baseapi.gcan.fault.dto;

import cn.ezios.baseapi.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FaultProfilePageQuery extends PageQuery {

    private String profileCode;
    private String profileName;
    private String manufacturer;
    private String vehicleType;
    private Integer status;
}
