package cn.ezios.baseapi.gcan.external;

import lombok.Data;

@Data
public class ExternalMineConfig {

    private Long id;
    private String mineCode;
    private String mineName;
    private String positionBaseUrl;
    private String positionApiPath;
    private String vehicleBaseUrl;
    private String vehicleApiPath;
    private String gcanBaseUrl;
    private String gcanApiPath;
    private String vehicleDataSource;
    private Integer pullFrequency;
    private Integer enabled;
}
