package cn.ezios.baseapi.gcan.fault.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("gcan_fault_definition")
public class GcanFaultDefinition {

    @TableId(type = IdType.AUTO)
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

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableLogic
    private Integer deleted;
}
