package cn.ezios.baseapi.gcan.state.controller;

import cn.ezios.baseapi.common.model.ApiResponse;
import cn.ezios.baseapi.gcan.state.dto.VehicleCanStateQuery;
import cn.ezios.baseapi.gcan.state.VehicleCanState;
import cn.ezios.baseapi.gcan.state.VehicleCanStateStore;
import cn.ezios.baseapi.gcan.vehicle.entity.GcanVehicle;
import cn.ezios.baseapi.gcan.vehicle.service.VehicleService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/gcan/vehicle-can-state")
public class VehicleCanStateController {

    private final VehicleCanStateStore stateStore;
    private final VehicleService vehicleService;

    public VehicleCanStateController(VehicleCanStateStore stateStore, VehicleService vehicleService) {
        this.stateStore = stateStore;
        this.vehicleService = vehicleService;
    }

    @GetMapping("/current")
    public ApiResponse<List<VehicleCanState>> current(VehicleCanStateQuery query) {
        Set<Long> vehicleIds = vehicleService.enabledVehicles(query).stream()
                .map(GcanVehicle::getId)
                .collect(Collectors.toSet());
        List<VehicleCanState> states = stateStore.currentStates().stream()
                .filter(state -> vehicleIds.contains(state.getVehicleId()))
                .filter(state -> query.getOnline() == null || Objects.equals(state.getOnline(), query.getOnline()))
                .toList();
        return ApiResponse.success(states);
    }
}
