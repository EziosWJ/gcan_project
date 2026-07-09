package cn.ezios.baseapi.gcan.raw.controller;

import cn.ezios.baseapi.common.model.ApiResponse;
import cn.ezios.baseapi.gcan.raw.service.RawCanFrameViewService;
import cn.ezios.baseapi.gcan.raw.vo.RawCanFrameVO;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gcan/raw-frame")
public class RawCanFrameController {

    private final RawCanFrameViewService rawCanFrameViewService;

    public RawCanFrameController(RawCanFrameViewService rawCanFrameViewService) {
        this.rawCanFrameViewService = rawCanFrameViewService;
    }

    @GetMapping("/current")
    public ApiResponse<List<RawCanFrameVO>> current(@RequestParam(required = false) String format) {
        return ApiResponse.success(rawCanFrameViewService.current(format));
    }

    @GetMapping(value = "/current/table", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    public String currentTable(@RequestParam(required = false) String format) {
        return rawCanFrameViewService.currentTable(format);
    }
}
