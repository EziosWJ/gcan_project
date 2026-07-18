package cn.ezios.baseapi.gcan.raw.mirror;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.ezios.baseapi.gcan.config.GcanProperties;
import cn.ezios.baseapi.gcan.raw.RawCanFrame;
import cn.ezios.baseapi.gcan.raw.RawCanFrameSnapshotStore;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

class GcanRawFrameMirrorSchedulerTest {

    @Test
    void enabledMirrorRequiresBaseUrlAndBoxIds() {
        GcanProperties properties = properties();
        properties.getMirror().setEnabled(true);
        properties.getMirror().setBaseUrl("");
        properties.getMirror().setBoxIds(List.of());
        GcanRawFrameMirrorScheduler scheduler = new GcanRawFrameMirrorScheduler(
                properties, new FakeSource(), new RawCanFrameSnapshotStore());

        assertThrows(IllegalStateException.class, scheduler::validateConfiguration);
    }

    @Test
    void mirrorsSelectedFramesAndPreservesSourceReceivedAt() {
        GcanProperties properties = properties();
        properties.getMirror().setBoxIds(List.of("0x1"));
        properties.getMirror().setCanIds(List.of("A001"));
        LocalDateTime receivedAt = LocalDateTime.now().minusSeconds(1);
        FakeSource source = new FakeSource();
        source.frames.put("01", List.of(
                frame("01", "A001", receivedAt),
                frame("01", "B002", receivedAt)));
        RawCanFrameSnapshotStore store = new RawCanFrameSnapshotStore();
        GcanRawFrameMirrorScheduler scheduler = scheduler(properties, source, store);

        scheduler.mirrorCurrentFrames();

        assertEquals(List.of("A001"), store.currentFrames().stream().map(RawCanFrame::getCanId).toList());
        assertEquals(receivedAt, store.currentFrames().getFirst().getReceivedAt());
    }

    @Test
    void successfulEmptyResponseClearsConfiguredBoxFrames() {
        GcanProperties properties = properties();
        properties.getMirror().setBoxIds(List.of("01"));
        FakeSource source = new FakeSource();
        RawCanFrameSnapshotStore store = new RawCanFrameSnapshotStore();
        store.put(frame("01", "A001", LocalDateTime.now()));
        GcanRawFrameMirrorScheduler scheduler = scheduler(properties, source, store);

        scheduler.mirrorCurrentFrames();

        assertEquals(List.of(), store.currentFrames());
    }

    @Test
    void failedBoxKeepsOldFramesWhileAnotherBoxUpdates() {
        GcanProperties properties = properties();
        properties.getMirror().setBoxIds(List.of("01", "02"));
        FakeSource source = new FakeSource();
        source.failureBoxes.add("01");
        source.frames.put("02", List.of(frame("02", "A002", LocalDateTime.now())));
        RawCanFrameSnapshotStore store = new RawCanFrameSnapshotStore();
        store.put(frame("01", "A001", LocalDateTime.now().minusSeconds(2)));
        GcanRawFrameMirrorScheduler scheduler = scheduler(properties, source, store);

        scheduler.mirrorCurrentFrames();

        assertEquals(List.of("01:A001", "02:A002"), store.currentFrames().stream()
                .map(frame -> frame.getBoxIdHex() + ":" + frame.getCanId())
                .toList());
    }

    private GcanRawFrameMirrorScheduler scheduler(GcanProperties properties,
                                                   GcanRawFrameSource source,
                                                   RawCanFrameSnapshotStore store) {
        GcanRawFrameMirrorScheduler scheduler = new GcanRawFrameMirrorScheduler(properties, source, store);
        scheduler.validateConfiguration();
        return scheduler;
    }

    private GcanProperties properties() {
        GcanProperties properties = new GcanProperties();
        properties.getMirror().setEnabled(true);
        properties.getMirror().setBaseUrl("http://mirror.test");
        return properties;
    }

    private RawCanFrame frame(String boxIdHex, String canId, LocalDateTime receivedAt) {
        return new RawCanFrame(boxIdHex, Integer.parseInt(boxIdHex, 16), canId,
                new int[]{1, 2, 3, 4, 5, 6, 7, 8}, receivedAt);
    }

    private static class FakeSource implements GcanRawFrameSource {
        private final Map<String, List<RawCanFrame>> frames = new ConcurrentHashMap<>();
        private final Collection<String> failureBoxes = ConcurrentHashMap.newKeySet();

        @Override
        public List<RawCanFrame> load(String boxIdHex) {
            if (failureBoxes.contains(boxIdHex)) {
                throw new IllegalStateException("simulated failure");
            }
            return frames.getOrDefault(boxIdHex, List.of());
        }
    }
}
