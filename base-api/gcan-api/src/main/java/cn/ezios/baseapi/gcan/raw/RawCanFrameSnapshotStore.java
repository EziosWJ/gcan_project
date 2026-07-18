package cn.ezios.baseapi.gcan.raw;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class RawCanFrameSnapshotStore {

    private final ConcurrentHashMap<String, RawCanFrame> frames = new ConcurrentHashMap<>();

    public void put(RawCanFrame frame) {
        frames.put(key(frame.getBoxIdHex(), frame.getCanId()), frame);
    }

    public synchronized void replaceFrames(String boxIdHex,
                                            Set<String> canIds,
                                            Collection<RawCanFrame> replacement) {
        frames.entrySet().removeIf(entry -> {
            RawCanFrame frame = entry.getValue();
            return frame.getBoxIdHex().equals(boxIdHex)
                    && (canIds.isEmpty() || canIds.contains(frame.getCanId()));
        });
        replacement.forEach(this::put);
    }

    public List<RawCanFrame> currentFrames() {
        return new ArrayList<>(frames.values()).stream()
                .sorted(Comparator.comparing(RawCanFrame::getBoxIdHex).thenComparing(RawCanFrame::getCanId))
                .toList();
    }

    public List<RawCanFrame> currentFramesByBox(String boxIdHex) {
        return currentFrames().stream()
                .filter(frame -> frame.getBoxIdHex().equals(boxIdHex))
                .toList();
    }

    public void clear() {
        frames.clear();
    }

    private String key(String boxIdHex, String canId) {
        return boxIdHex + ":" + canId;
    }
}
