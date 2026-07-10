package cn.ezios.baseapi.gcan.vehicle.controller;

import cn.ezios.baseapi.common.model.ApiResponse;
import cn.ezios.baseapi.common.model.BatchIdsRequest;
import cn.ezios.baseapi.common.model.PageResult;
import cn.ezios.baseapi.common.model.StatusUpdateRequest;
import cn.ezios.baseapi.gcan.vehicle.dto.VehiclePageQuery;
import cn.ezios.baseapi.gcan.vehicle.dto.VehicleSaveRequest;
import cn.ezios.baseapi.gcan.vehicle.service.VehicleService;
import cn.ezios.baseapi.gcan.vehicle.vo.VehicleVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/gcan/vehicle")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping("/page")
    public ApiResponse<PageResult<VehicleVO>> page(@Valid VehiclePageQuery query) {
        return ApiResponse.success(vehicleService.page(query));
    }

    @GetMapping("/enabled")
    public ApiResponse<List<VehicleVO>> enabled() {
        return ApiResponse.success(vehicleService.listEnabled());
    }

    @GetMapping("/{id}")
    public ApiResponse<VehicleVO> detail(@PathVariable Long id) {
        return ApiResponse.success(vehicleService.getDetail(id));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody VehicleSaveRequest request) {
        vehicleService.create(request);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody VehicleSaveRequest request) {
        vehicleService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        vehicleService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/batch-delete")
    public ApiResponse<Void> deleteBatch(@Valid @RequestBody BatchIdsRequest request) {
        vehicleService.deleteBatch(request);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        vehicleService.updateStatus(id, request);
        return ApiResponse.success();
    }
}
