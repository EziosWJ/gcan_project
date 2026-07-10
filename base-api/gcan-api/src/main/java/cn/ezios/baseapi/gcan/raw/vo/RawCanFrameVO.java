package cn.ezios.baseapi.gcan.raw.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class RawCanFrameVO {

    private Long vehicleId;
    private String vehicleName;
    private String mineId;
    private String vehicleType;
    private String boxIdHex;
    private Integer boxIdDec;
    private String canId;
    private List<String> data;
    private LocalDateTime receivedAt;
}
