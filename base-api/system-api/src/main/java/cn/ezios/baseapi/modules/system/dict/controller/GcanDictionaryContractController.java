package cn.ezios.baseapi.modules.system.dict.controller;

import cn.ezios.baseapi.common.model.ApiResponse;
import cn.ezios.baseapi.modules.system.dict.contract.GcanDictionaryVO;
import cn.ezios.baseapi.modules.system.dict.service.DictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "GCAN字典契约")
@RestController
@RequestMapping("/api/open/gcan/v1/dictionaries")
public class GcanDictionaryContractController {

    private final DictService dictService;

    public GcanDictionaryContractController(DictService dictService) {
        this.dictService = dictService;
    }

    @Operation(summary = "查询GCAN字典名称")
    @GetMapping
    public ApiResponse<List<GcanDictionaryVO>> all() {
        return ApiResponse.success(dictService.gcanItems());
    }
}
