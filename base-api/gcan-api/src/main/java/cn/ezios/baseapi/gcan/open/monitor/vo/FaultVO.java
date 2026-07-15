package cn.ezios.baseapi.gcan.open.monitor.vo;

import lombok.Data;

@Data
public class FaultVO {

    private boolean active;
    private boolean stale;
    private boolean configured;
    private boolean matched;
    private String code;
    private String levelCode;
    private String levelName;
    private String name;
    private String definition;
    private String description;
    private String analysis;
    private String symptom;
    private String recovery;
    private String clear;
    private String handlingAdvice;
    private String suggestion;
}
