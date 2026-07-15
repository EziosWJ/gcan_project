package cn.ezios.baseapi.gcan.dictionary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GcanDictionaryCacheTest {

    private GcanDictionaryCache cache;

    @AfterEach
    void closeCache() {
        if (cache != null) {
            cache.close();
        }
    }

    @Test
    void refreshLoadsNamesForAllContractDictionaries() {
        FakeSource source = new FakeSource();
        cache = new GcanDictionaryCache(source);

        assertTrue(cache.refreshNow());
        assertEquals("一号煤矿", cache.name(GcanDictionaryCodes.MINE, "MINE_1"));
        assertEquals("在线", cache.name(GcanDictionaryCodes.VEHICLE_CONNECTION_STATUS, "ONLINE"));
        assertEquals("UNKNOWN", cache.name(GcanDictionaryCodes.VEHICLE_TYPE, "UNKNOWN"));
    }

    @Test
    void failedRefreshKeepsOldCacheAndFallsBackToCode() {
        FakeSource source = new FakeSource();
        cache = new GcanDictionaryCache(source);
        assertTrue(cache.refreshNow());

        source.failure = true;
        assertFalse(cache.refreshNow());
        assertEquals("一号煤矿", cache.name(GcanDictionaryCodes.MINE, "MINE_1"));
        assertEquals("NEW_MINE", cache.name(GcanDictionaryCodes.MINE, "NEW_MINE"));
    }

    @Test
    void unknownCodeTriggersBackgroundRefreshWithoutChangingFallback() throws Exception {
        CountDownLatch refreshStarted = new CountDownLatch(1);
        FakeSource source = new FakeSource();
        source.onLoad = refreshStarted::countDown;
        cache = new GcanDictionaryCache(source);

        assertEquals("MINE_1", cache.name(GcanDictionaryCodes.MINE, "MINE_1"));
        assertTrue(refreshStarted.await(1, TimeUnit.SECONDS));
        assertTrue(source.loadCount.get() >= 1);
    }

    private static class FakeSource implements GcanDictionarySource {

        private final AtomicInteger loadCount = new AtomicInteger();
        private boolean failure;
        private Runnable onLoad;

        @Override
        public List<GcanDictionaryGroup> load() {
            loadCount.incrementAndGet();
            if (onLoad != null) {
                onLoad.run();
            }
            if (failure) {
                throw new IllegalStateException("test failure");
            }
            List<GcanDictionaryGroup> groups = new ArrayList<>();
            groups.add(group(GcanDictionaryCodes.MINE, item("MINE_1", "一号煤矿")));
            groups.add(group(GcanDictionaryCodes.VEHICLE_TYPE, item("LIAO_1_9T", "1.9T料车")));
            groups.add(group(GcanDictionaryCodes.VEHICLE_CONNECTION_STATUS,
                    item("ONLINE", "在线"), item("OFFLINE", "离线"), item("NO_DATA", "暂无数据")));
            groups.add(group(GcanDictionaryCodes.VEHICLE_PARSE_STATUS,
                    item("SUPPORTED", "已支持解析"), item("UNSUPPORTED", "未支持解析")));
            return groups;
        }

        private static GcanDictionaryGroup group(String code, GcanDictionaryItem... items) {
            GcanDictionaryGroup group = new GcanDictionaryGroup();
            group.setDictCode(code);
            group.setItems(List.of(items));
            return group;
        }

        private static GcanDictionaryItem item(String code, String name) {
            GcanDictionaryItem item = new GcanDictionaryItem();
            item.setCode(code);
            item.setName(name);
            return item;
        }
    }
}
