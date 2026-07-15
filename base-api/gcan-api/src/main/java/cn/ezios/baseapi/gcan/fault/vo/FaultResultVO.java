package cn.ezios.baseapi.gcan.fault.vo;

import lombok.Data;

@Data
public class FaultResultVO {

    public static final String NO_FAULT = "NO_FAULT";
    public static final String UNCONFIGURED_PROFILE = "UNCONFIGURED_PROFILE";
    public static final String UNKNOWN_FAULT = "UNKNOWN_FAULT";
    public static final String MATCHED = "MATCHED";

    private String status;
    private String faultProfileCode;
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
}
