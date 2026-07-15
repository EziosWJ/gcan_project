package cn.ezios.baseapi.gcan.open.monitor.vo;

import java.util.List;
import lombok.Data;

@Data
public class MonitorMineVO {

    private String mineId;
    private String mineName;
    private Integer sortOrder;
    private MonitorStatsVO statistics;
    private List<PublicVehicleStateVO> vehicles;
}
