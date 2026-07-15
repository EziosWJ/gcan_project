package cn.ezios.baseapi.gcan.open.monitor.vo;

import lombok.Data;

@Data
public class MonitorStatsVO {

    private long vehicleTotal;
    private long onlineCount;
    private long offlineCount;
    private long noDataCount;
    private long unsupportedCount;
    private long faultVehicleCount;
    private java.time.LocalDateTime latestDataAt;
}
