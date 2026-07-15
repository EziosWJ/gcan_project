package cn.ezios.baseapi.gcan.open.controller;

import cn.ezios.baseapi.common.model.ApiResponse;
import cn.ezios.baseapi.common.model.PageResult;
import cn.ezios.baseapi.gcan.dictionary.GcanDictionaryNameService;
import cn.ezios.baseapi.gcan.dictionary.GcanDictionaryItem;
import cn.ezios.baseapi.gcan.open.monitor.service.MonitorOverviewService;
import cn.ezios.baseapi.gcan.open.monitor.vo.MonitorOverviewVO;
import cn.ezios.baseapi.gcan.raw.dto.RawCanFrameQuery;
import cn.ezios.baseapi.gcan.raw.dto.RawCanHistoryPageQuery;
import cn.ezios.baseapi.gcan.raw.service.RawCanFrameViewService;
import cn.ezios.baseapi.gcan.raw.vo.RawCanFrameVO;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/open/gcan/v1")
public class PublicGcanController {

    private static final long MAX_PAGE_SIZE = 200;

    private final MonitorOverviewService monitorOverviewService;
    private final RawCanFrameViewService rawCanFrameViewService;
    private final GcanDictionaryNameService dictionaryNameService;

    public PublicGcanController(MonitorOverviewService monitorOverviewService,
                                RawCanFrameViewService rawCanFrameViewService,
                                GcanDictionaryNameService dictionaryNameService) {
        this.monitorOverviewService = monitorOverviewService;
        this.rawCanFrameViewService = rawCanFrameViewService;
        this.dictionaryNameService = dictionaryNameService;
    }

    @GetMapping("/monitor/overview")
    public ApiResponse<MonitorOverviewVO> overview() {
        return ApiResponse.success(monitorOverviewService.overview());
    }

    @GetMapping("/dictionary/{dictCode}")
    public ApiResponse<List<GcanDictionaryItem>> dictionary(@PathVariable String dictCode) {
        return ApiResponse.success(dictionaryNameService.items(dictCode));
    }

    @GetMapping("/vehicle-can-state/current")
    public ApiResponse<List<cn.ezios.baseapi.gcan.state.VehicleCanState>> current(RawCanFrameQuery query) {
        return ApiResponse.success(monitorOverviewService.overview().getMines().stream()
                .flatMap(mine -> mine.getVehicles().stream())
                .map(item -> (cn.ezios.baseapi.gcan.state.VehicleCanState) item)
                .filter(state -> matches(state, query))
                .toList());
    }

    @GetMapping("/raw-frame/current")
    public ApiResponse<List<RawCanFrameVO>> rawCurrent(RawCanFrameQuery query,
                                                       @RequestParam(required = false) String format) {
        return ApiResponse.success(rawCanFrameViewService.current(query, format));
    }

    @GetMapping("/raw-frame/history/page")
    public ApiResponse<PageResult<RawCanFrameVO>> rawHistory(RawCanHistoryPageQuery query,
                                                             @RequestParam(required = false) String format) {
        validateHistory(query);
        if (query.getPageSize() == 10) {
            query.setPageSize(50);
        }
        query.setPageSize(Math.min(query.getPageSize(), MAX_PAGE_SIZE));
        return ApiResponse.success(rawCanFrameViewService.historyPage(query, format));
    }

    private boolean matches(cn.ezios.baseapi.gcan.state.VehicleCanState state, RawCanFrameQuery query) {
        return (!StringUtils.hasText(query.getVehicleName()) || state.getVehicleName().contains(query.getVehicleName()))
                && (!StringUtils.hasText(query.getMineId()) || query.getMineId().equals(state.getMineId()))
                && (!StringUtils.hasText(query.getVehicleType()) || query.getVehicleType().equalsIgnoreCase(state.getVehicleType()))
                && (!StringUtils.hasText(query.getBoxIdHex()) || query.getBoxIdHex().equalsIgnoreCase(state.getBoxIdHex()));
    }

    private void validateHistory(RawCanHistoryPageQuery query) {
        if (query.getReceivedStart() == null || query.getReceivedEnd() == null) {
            throw new IllegalArgumentException("历史查询必须提供 receivedStart 和 receivedEnd");
        }
        if (query.getReceivedEnd().isBefore(query.getReceivedStart())) {
            throw new IllegalArgumentException("receivedEnd 不能早于 receivedStart");
        }
        if (Duration.between(query.getReceivedStart(), query.getReceivedEnd()).toHours() > 24) {
            throw new IllegalArgumentException("历史查询时间范围不能超过 24 小时");
        }
    }
}
