package cn.ezios.baseapi.gcan.fault.controller;

import cn.ezios.baseapi.common.model.ApiResponse;
import cn.ezios.baseapi.common.model.BatchIdsRequest;
import cn.ezios.baseapi.common.model.PageResult;
import cn.ezios.baseapi.common.model.StatusUpdateRequest;
import cn.ezios.baseapi.gcan.fault.dto.FaultProfilePageQuery;
import cn.ezios.baseapi.gcan.fault.dto.FaultProfileSaveRequest;
import cn.ezios.baseapi.gcan.fault.service.FaultProfileService;
import cn.ezios.baseapi.gcan.fault.vo.FaultProfileVO;
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
@RequestMapping("/api/gcan/fault-profile")
public class FaultProfileController {

    private final FaultProfileService faultProfileService;

    public FaultProfileController(FaultProfileService faultProfileService) {
        this.faultProfileService = faultProfileService;
    }

    @GetMapping("/page")
    public ApiResponse<PageResult<FaultProfileVO>> page(@Valid FaultProfilePageQuery query) {
        return ApiResponse.success(faultProfileService.page(query));
    }

    @GetMapping("/enabled")
    public ApiResponse<List<FaultProfileVO>> enabled() {
        return ApiResponse.success(faultProfileService.listEnabled());
    }

    @GetMapping("/{id}")
    public ApiResponse<FaultProfileVO> detail(@PathVariable Long id) {
        return ApiResponse.success(faultProfileService.getDetail(id));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody FaultProfileSaveRequest request) {
        faultProfileService.create(request);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody FaultProfileSaveRequest request) {
        faultProfileService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        faultProfileService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/batch-delete")
    public ApiResponse<Void> deleteBatch(@Valid @RequestBody BatchIdsRequest request) {
        faultProfileService.deleteBatch(request);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        faultProfileService.updateStatus(id, request);
        return ApiResponse.success();
    }
}
