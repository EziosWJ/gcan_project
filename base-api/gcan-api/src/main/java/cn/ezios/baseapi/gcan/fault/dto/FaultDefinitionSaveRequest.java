package cn.ezios.baseapi.gcan.fault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FaultDefinitionSaveRequest {

    @NotBlank(message = "故障码表编码不能为空")
    @Size(max = 100, message = "故障码表编码长度不能超过 100")
    private String profileCode;

    @NotBlank(message = "故障码不能为空")
    @Size(max = 100, message = "故障码长度不能超过 100")
    private String faultCode;

    @Size(max = 50, message = "原始等级编码长度不能超过 50")
    private String rawLevelCode;

    @Size(max = 100, message = "原始等级名称长度不能超过 100")
    private String rawLevelName;

    @Size(max = 200, message = "故障名称长度不能超过 200")
    private String faultName;

    @Size(max = 2000, message = "故障定义长度不能超过 2000")
    private String faultDefinition;

    @Size(max = 2000, message = "解析长度不能超过 2000")
    private String analysis;

    @Size(max = 2000, message = "表现长度不能超过 2000")
    private String symptom;

    @Size(max = 2000, message = "恢复长度不能超过 2000")
    private String recovery;

    @Size(max = 2000, message = "解除长度不能超过 2000")
    private String removal;

    @Size(max = 2000, message = "处理建议长度不能超过 2000")
    private String handlingSuggestion;

    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;

    private Integer status;
}
