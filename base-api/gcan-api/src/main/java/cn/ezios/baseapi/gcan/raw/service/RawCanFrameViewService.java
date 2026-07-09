package cn.ezios.baseapi.gcan.raw.service;

import cn.ezios.baseapi.gcan.common.ByteFormat;
import cn.ezios.baseapi.gcan.raw.RawCanFrame;
import cn.ezios.baseapi.gcan.raw.RawCanFrameSnapshotStore;
import cn.ezios.baseapi.gcan.raw.vo.RawCanFrameVO;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RawCanFrameViewService {

    private static final DateTimeFormatter TABLE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final RawCanFrameSnapshotStore snapshotStore;

    public RawCanFrameViewService(RawCanFrameSnapshotStore snapshotStore) {
        this.snapshotStore = snapshotStore;
    }

    public List<RawCanFrameVO> current(String format) {
        ByteFormat byteFormat = ByteFormat.parse(format);
        return snapshotStore.currentFrames().stream().map(frame -> toVO(frame, byteFormat)).toList();
    }

    public String currentTable(String format) {
        ByteFormat byteFormat = ByteFormat.parse(format);
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%-8s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-24s%n",
                "BOX_HEX", "BOX_DEC", "CAN_ID", "DATA0", "DATA1", "DATA2", "DATA3",
                "DATA4", "DATA5", "DATA6", "DATA7", "RECEIVED_AT"));
        for (RawCanFrame frame : snapshotStore.currentFrames()) {
            int[] values = frame.values();
            builder.append(String.format("%-8s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-24s%n",
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

    private RawCanFrameVO toVO(RawCanFrame frame, ByteFormat byteFormat) {
        RawCanFrameVO vo = new RawCanFrameVO();
        vo.setBoxIdHex(frame.getBoxIdHex());
        vo.setBoxIdDec(frame.getBoxIdDec());
        vo.setCanId(frame.getCanId());
        vo.setData(Arrays.stream(frame.values()).mapToObj(byteFormat::format).toList());
        vo.setReceivedAt(frame.getReceivedAt());
        return vo;
    }
}
