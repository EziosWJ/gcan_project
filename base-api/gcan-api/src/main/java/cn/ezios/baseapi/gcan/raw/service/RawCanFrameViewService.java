package cn.ezios.baseapi.gcan.raw.service;

import cn.ezios.baseapi.common.model.PageResult;
import cn.ezios.baseapi.gcan.common.ByteFormat;
import cn.ezios.baseapi.gcan.common.BoxIdUtil;
import cn.ezios.baseapi.gcan.history.entity.GcanCanHistory;
import cn.ezios.baseapi.gcan.history.mapper.GcanCanHistoryMapper;
import cn.ezios.baseapi.gcan.raw.RawCanFrame;
import cn.ezios.baseapi.gcan.raw.RawCanFrameSnapshotStore;
import cn.ezios.baseapi.gcan.raw.dto.RawCanFrameQuery;
import cn.ezios.baseapi.gcan.raw.dto.RawCanHistoryPageQuery;
import cn.ezios.baseapi.gcan.raw.vo.RawCanFrameVO;
import cn.ezios.baseapi.gcan.vehicle.entity.GcanVehicle;
import cn.ezios.baseapi.gcan.vehicle.service.VehicleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RawCanFrameViewService {

    private static final DateTimeFormatter TABLE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final RawCanFrameSnapshotStore snapshotStore;
    private final GcanCanHistoryMapper historyMapper;
    private final VehicleService vehicleService;

    public RawCanFrameViewService(RawCanFrameSnapshotStore snapshotStore,
                                  GcanCanHistoryMapper historyMapper,
                                  VehicleService vehicleService) {
        this.snapshotStore = snapshotStore;
        this.historyMapper = historyMapper;
        this.vehicleService = vehicleService;
    }

    public List<RawCanFrameVO> current(String format) {
        return current(new RawCanFrameQuery(), format);
    }

    public List<RawCanFrameVO> current(RawCanFrameQuery query, String format) {
        ByteFormat byteFormat = ByteFormat.parse(format);
        Map<String, GcanVehicle> vehiclesByBox = vehicleService.byBoxIdHex(query.toVehicleLookupQuery());
        boolean restrictByVehicle = hasVehicleContextFilter(query);
        String boxIdHex = normalizeBoxId(query.getBoxIdHex());
        String canId = normalizeCanId(query.getCanId());
        return snapshotStore.currentFrames().stream()
                .filter(frame -> !StringUtils.hasText(boxIdHex) || frame.getBoxIdHex().equals(boxIdHex))
                .filter(frame -> !StringUtils.hasText(canId) || frame.getCanId().equals(canId))
                .filter(frame -> !restrictByVehicle || vehiclesByBox.containsKey(frame.getBoxIdHex()))
                .map(frame -> toVO(frame, vehiclesByBox.get(frame.getBoxIdHex()), byteFormat))
                .toList();
    }

    public String currentTable(String format) {
        return currentTable(new RawCanFrameQuery(), format);
    }

    public String currentTable(RawCanFrameQuery query, String format) {
        ByteFormat byteFormat = ByteFormat.parse(format);
        Map<String, GcanVehicle> vehiclesByBox = vehicleService.byBoxIdHex(query.toVehicleLookupQuery());
        List<RawCanFrame> frames = currentFrames(query, vehiclesByBox);
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%-18s %-12s %-14s %-8s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-24s%n",
                "VEHICLE", "MINE", "TYPE", "BOX_HEX", "BOX_DEC", "CAN_ID", "DATA0", "DATA1", "DATA2", "DATA3",
                "DATA4", "DATA5", "DATA6", "DATA7", "RECEIVED_AT"));
        for (RawCanFrame frame : frames) {
            int[] values = frame.values();
            GcanVehicle vehicle = vehiclesByBox.get(frame.getBoxIdHex());
            builder.append(String.format("%-18s %-12s %-14s %-8s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-24s%n",
                    textValue(vehicle == null ? null : vehicle.getVehicleName()),
                    textValue(vehicle == null ? null : vehicle.getMineId()),
                    textValue(vehicle == null ? null : vehicle.getVehicleType()),
                    frame.getBoxIdHex(),
                    frame.getBoxIdDec(),
                    frame.getCanId(),
                    byteFormat.format(values[0]),
                    byteFormat.format(values[1]),
                    byteFormat.format(values[2]),
                    byteFormat.format(values[3]),
                    byteFormat.format(values[4]),
                    byteFormat.format(values[5]),
                    byteFormat.format(values[6]),
                    byteFormat.format(values[7]),
                    frame.getReceivedAt().format(TABLE_TIME_FORMATTER)));
        }
        return builder.toString();
    }

    public PageResult<RawCanFrameVO> historyPage(RawCanHistoryPageQuery query, String format) {
        ByteFormat byteFormat = ByteFormat.parse(format);
        Map<String, GcanVehicle> vehiclesByBox = vehicleService.byBoxIdHex(query.toVehicleLookupQuery());
        Set<Long> vehicleIds = vehiclesByBox.values().stream()
                .map(GcanVehicle::getId)
                .collect(Collectors.toSet());
        boolean restrictByVehicle = hasVehicleContextFilter(query);
        if (restrictByVehicle && vehicleIds.isEmpty()) {
            return new PageResult<>(List.of(), 0, query.getPage(), query.getPageSize());
        }

        LocalDateTime end = query.getReceivedEnd() == null ? LocalDateTime.now() : query.getReceivedEnd();
        LocalDateTime start = query.getReceivedStart() == null ? end.minusHours(1) : query.getReceivedStart();
        String boxIdHex = normalizeBoxId(query.getBoxIdHex());
        String canId = normalizeCanId(query.getCanId());
        Page<GcanCanHistory> page = historyMapper.selectPage(Page.of(query.getPage(), query.getPageSize()),
                new LambdaQueryWrapper<GcanCanHistory>()
                        .eq(StringUtils.hasText(boxIdHex), GcanCanHistory::getBoxIdHex, boxIdHex)
                        .eq(StringUtils.hasText(canId), GcanCanHistory::getCanId, canId)
                        .in(restrictByVehicle, GcanCanHistory::getVehicleId, vehicleIds)
                        .ge(GcanCanHistory::getReceivedAt, start)
                        .le(GcanCanHistory::getReceivedAt, end)
                        .orderByDesc(GcanCanHistory::getReceivedAt)
                        .orderByDesc(GcanCanHistory::getId));
        List<RawCanFrameVO> records = page.getRecords().stream()
                .map(history -> toVO(history, vehiclesByBox.get(history.getBoxIdHex()), byteFormat))
                .toList();
        return new PageResult<>(records, page.getTotal(), query.getPage(), query.getPageSize());
    }

    private List<RawCanFrame> currentFrames(RawCanFrameQuery query, Map<String, GcanVehicle> vehiclesByBox) {
        boolean restrictByVehicle = hasVehicleContextFilter(query);
        String boxIdHex = normalizeBoxId(query.getBoxIdHex());
        String canId = normalizeCanId(query.getCanId());
        return snapshotStore.currentFrames().stream()
                .filter(frame -> !StringUtils.hasText(boxIdHex) || frame.getBoxIdHex().equals(boxIdHex))
                .filter(frame -> !StringUtils.hasText(canId) || frame.getCanId().equals(canId))
                .filter(frame -> !restrictByVehicle || vehiclesByBox.containsKey(frame.getBoxIdHex()))
                .toList();
    }

    private RawCanFrameVO toVO(RawCanFrame frame, GcanVehicle vehicle, ByteFormat byteFormat) {
        RawCanFrameVO vo = new RawCanFrameVO();
        fillVehicle(vo, vehicle);
        vo.setBoxIdHex(frame.getBoxIdHex());
        vo.setBoxIdDec(frame.getBoxIdDec());
        vo.setCanId(frame.getCanId());
        vo.setData(Arrays.stream(frame.values()).mapToObj(byteFormat::format).toList());
        vo.setReceivedAt(frame.getReceivedAt());
        return vo;
    }

    private RawCanFrameVO toVO(GcanCanHistory history, GcanVehicle vehicle, ByteFormat byteFormat) {
        RawCanFrameVO vo = new RawCanFrameVO();
        fillVehicle(vo, vehicle);
        vo.setBoxIdHex(history.getBoxIdHex());
        vo.setBoxIdDec(history.getBoxIdDec());
        vo.setCanId(history.getCanId());
        vo.setData(List.of(
                byteFormat.format(history.getValue0()),
                byteFormat.format(history.getValue1()),
                byteFormat.format(history.getValue2()),
                byteFormat.format(history.getValue3()),
                byteFormat.format(history.getValue4()),
                byteFormat.format(history.getValue5()),
                byteFormat.format(history.getValue6()),
                byteFormat.format(history.getValue7())
        ));
        vo.setReceivedAt(history.getReceivedAt());
        return vo;
    }

    private void fillVehicle(RawCanFrameVO vo, GcanVehicle vehicle) {
        if (vehicle == null) {
            return;
        }
        vo.setVehicleId(vehicle.getId());
        vo.setVehicleName(vehicle.getVehicleName());
        vo.setMineId(vehicle.getMineId());
        vo.setVehicleType(vehicle.getVehicleType());
    }

    private boolean hasVehicleContextFilter(RawCanFrameQuery query) {
        return StringUtils.hasText(query.getVehicleName())
                || StringUtils.hasText(query.getMineId())
                || StringUtils.hasText(query.getVehicleType());
    }

    private String normalizeBoxId(String boxIdHex) {
        return StringUtils.hasText(boxIdHex) ? BoxIdUtil.normalizeHex(boxIdHex) : null;
    }

    private String normalizeCanId(String canId) {
        return StringUtils.hasText(canId) ? canId.trim().toUpperCase() : null;
    }

    private String textValue(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }
}
