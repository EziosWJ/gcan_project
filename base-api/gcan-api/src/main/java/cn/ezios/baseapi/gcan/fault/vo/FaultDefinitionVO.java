package cn.ezios.baseapi.gcan.fault.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FaultDefinitionVO {

    private Long id;
    private String profileCode;
    private String faultCode;
    private String rawLevelCode;
    private String rawLevelName;
    private String faultName;
    private String faultDefinition;
    private String analysis;
    private String symptom;
    private String recovery;
    private String removal;
    private String handlingSuggestion;
    private String remark;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
