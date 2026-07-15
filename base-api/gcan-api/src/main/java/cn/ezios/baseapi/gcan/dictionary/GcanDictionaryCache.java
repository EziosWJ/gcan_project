package cn.ezios.baseapi.gcan.dictionary;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GcanDictionaryCache implements GcanDictionaryNameService {

    private final GcanDictionarySource source;
    private final AtomicReference<Map<String, Map<String, String>>> cache =
            new AtomicReference<>(Map.of());
    private final AtomicBoolean refreshInProgress = new AtomicBoolean();
    private final ExecutorService refreshExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "gcan-dictionary-refresh");
        thread.setDaemon(true);
        return thread;
    });

    public GcanDictionaryCache(GcanDictionarySource source) {
        this.source = source;
    }

    @PostConstruct
    public void loadOnStartup() {
        refreshAsync();
    }

    @Scheduled(fixedDelayString = "${gcan.dictionary.refresh-interval-ms:60000}")
    public void refreshOnSchedule() {
        refreshNow();
    }

    @Override
    public String name(String dictCode, String code) {
        return resolve(dictCode, code).name();
    }

    @Override
    public GcanDictionaryName resolve(String dictCode, String code) {
        if (code == null || code.isBlank()) {
            return new GcanDictionaryName(code, code);
        }
        String normalizedCode = code.trim();
        String normalizedDictCode = dictCode == null ? null : dictCode.trim();
        Map<String, String> dictionary = cache.get().get(normalizedDictCode);
        String name = dictionary == null ? null : dictionary.get(normalizedCode);
        if (name == null) {
            refreshAsync();
            return new GcanDictionaryName(normalizedCode, normalizedCode);
        }
        return new GcanDictionaryName(normalizedCode, name);
    }

    @Override
    public List<GcanDictionaryItem> items(String dictCode) {
        Map<String, String> dictionary = cache.get().get(dictCode);
        if (dictionary == null) {
            refreshAsync();
            return List.of();
        }
        return dictionary.entrySet().stream()
                .map(entry -> {
                    GcanDictionaryItem item = new GcanDictionaryItem();
                    item.setCode(entry.getKey());
                    item.setName(entry.getValue());
                    return item;
                })
                .toList();
    }

    @Override
    public Map<String, Map<String, String>> snapshot() {
        return cache.get();
    }

    @Override
    public synchronized boolean refreshNow() {
        try {
            Map<String, Map<String, String>> refreshed = toCache(source.load());
            cache.set(refreshed);
            return true;
        } catch (RuntimeException exception) {
            log.warn("GCAN字典刷新失败，继续使用旧缓存", exception);
            return false;
        }
    }

    @Override
    public void refreshAsync() {
        if (!refreshInProgress.compareAndSet(false, true)) {
            return;
        }
        refreshExecutor.execute(() -> {
            try {
                refreshNow();
            } finally {
                refreshInProgress.set(false);
            }
        });
    }

    @PreDestroy
    public void close() {
        refreshExecutor.shutdownNow();
    }

    private Map<String, Map<String, String>> toCache(List<GcanDictionaryGroup> groups) {
        if (groups == null) {
            throw new IllegalStateException("GCAN字典契约缺少数据");
        }
        Map<String, Map<String, String>> refreshed = new LinkedHashMap<>();
        for (GcanDictionaryGroup group : groups) {
            if (group == null || group.getDictCode() == null || group.getDictCode().isBlank()
                    || group.getItems() == null) {
                throw new IllegalStateException("GCAN字典契约数据不完整");
            }
            String dictCode = group.getDictCode().trim();
            if (!GcanDictionaryCodes.ALL.contains(dictCode)) {
                throw new IllegalStateException("GCAN字典契约包含未授权字典: " + dictCode);
            }
            Map<String, String> items = new LinkedHashMap<>();
            for (GcanDictionaryItem item : group.getItems()) {
                if (item == null || item.getCode() == null || item.getCode().isBlank()
                        || item.getName() == null || item.getName().isBlank()) {
                    throw new IllegalStateException("GCAN字典项编码或名称为空");
                }
                items.put(item.getCode().trim(), item.getName());
            }
            if (refreshed.put(dictCode, Collections.unmodifiableMap(items)) != null) {
                throw new IllegalStateException("GCAN字典契约包含重复字典: " + dictCode);
            }
        }
        if (!refreshed.keySet().containsAll(GcanDictionaryCodes.ALL)) {
            throw new IllegalStateException("GCAN字典契约缺少必要字典");
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(refreshed));
    }
}
