package cn.ezios.baseapi.gcan.open.monitor.vo;

import cn.ezios.baseapi.gcan.state.VehicleCanState;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PublicVehicleStateVO extends VehicleCanState {

    private String mineName;
    private String connectionStatusLabel;
    private String parseStatusLabel;
    private FaultVO fault;
    private List<String> supportedUnits;
}
