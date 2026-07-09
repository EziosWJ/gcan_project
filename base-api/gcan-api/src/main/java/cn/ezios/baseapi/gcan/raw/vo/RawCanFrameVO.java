package cn.ezios.baseapi.gcan.raw.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class RawCanFrameVO {

    private String boxIdHex;
    private Integer boxIdDec;
    private String canId;
    private List<String> data;
    private LocalDateTime receivedAt;
}
