package cn.ezios.baseapi.gcan.state.controller;

import cn.ezios.baseapi.common.model.ApiResponse;
import cn.ezios.baseapi.gcan.state.VehicleCanState;
import cn.ezios.baseapi.gcan.state.VehicleCanStateStore;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gcan/vehicle-can-state")
public class VehicleCanStateController {

    private final VehicleCanStateStore stateStore;

    public VehicleCanStateController(VehicleCanStateStore stateStore) {
        this.stateStore = stateStore;
    }

    @GetMapping("/current")
    public ApiResponse<List<VehicleCanState>> current() {
        return ApiResponse.success(stateStore.currentStates());
    }
}
