package cn.ezios.baseapi.gcan.raw.dto;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

@Data
@EqualsAndHashCode(callSuper = true)
public class RawCanHistoryPageQuery extends RawCanFrameQuery {

    @DateTimeFormat(iso = ISO.DATE_TIME)
    private LocalDateTime receivedStart;

    @DateTimeFormat(iso = ISO.DATE_TIME)
    private LocalDateTime receivedEnd;
}
