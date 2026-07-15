package cn.ezios.baseapi.gcan.open.monitor.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class MonitorOverviewVO {

    private LocalDateTime generatedAt;
    private LocalDateTime lastUpdateAt;
    private MonitorStatsVO statistics;
    private List<MonitorMineVO> mines;
}
