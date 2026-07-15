package cn.ezios.baseapi.gcan.fault.dto;

import cn.ezios.baseapi.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FaultDefinitionPageQuery extends PageQuery {

    private String profileCode;
    private String faultCode;
    private String faultName;
    private Integer status;
}
