package cn.ezios.baseapi.gcan.history.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("gcan_can_history")
public class GcanCanHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long vehicleId;
    private String boxIdHex;
    private Integer boxIdDec;
    private String canId;
    private Integer value0;
    private Integer value1;
    private Integer value2;
    private Integer value3;
    private Integer value4;
    private Integer value5;
    private Integer value6;
    private Integer value7;
    private LocalDateTime receivedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
