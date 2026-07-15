package cn.ezios.baseapi.gcan.fault.controller;

import cn.ezios.baseapi.common.model.ApiResponse;
import cn.ezios.baseapi.common.model.BatchIdsRequest;
import cn.ezios.baseapi.common.model.PageResult;
import cn.ezios.baseapi.common.model.StatusUpdateRequest;
import cn.ezios.baseapi.gcan.fault.dto.FaultDefinitionPageQuery;
import cn.ezios.baseapi.gcan.fault.dto.FaultDefinitionSaveRequest;
import cn.ezios.baseapi.gcan.fault.service.FaultDefinitionService;
import cn.ezios.baseapi.gcan.fault.vo.FaultDefinitionVO;
import jakarta.validation.Valid;
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
@RequestMapping("/api/gcan/fault-definition")
public class FaultDefinitionController {

    private final FaultDefinitionService faultDefinitionService;

    public FaultDefinitionController(FaultDefinitionService faultDefinitionService) {
        this.faultDefinitionService = faultDefinitionService;
    }

    @GetMapping("/page")
    public ApiResponse<PageResult<FaultDefinitionVO>> page(@Valid FaultDefinitionPageQuery query) {
        return ApiResponse.success(faultDefinitionService.page(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<FaultDefinitionVO> detail(@PathVariable Long id) {
        return ApiResponse.success(faultDefinitionService.getDetail(id));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody FaultDefinitionSaveRequest request) {
        faultDefinitionService.create(request);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody FaultDefinitionSaveRequest request) {
        faultDefinitionService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        faultDefinitionService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/batch-delete")
    public ApiResponse<Void> deleteBatch(@Valid @RequestBody BatchIdsRequest request) {
        faultDefinitionService.deleteBatch(request);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        faultDefinitionService.updateStatus(id, request);
        return ApiResponse.success();
    }
}
