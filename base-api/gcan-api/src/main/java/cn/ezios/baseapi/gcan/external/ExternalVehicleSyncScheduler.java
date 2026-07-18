package cn.ezios.baseapi.gcan.external;

import cn.ezios.baseapi.gcan.config.ExternalSourceConfigStore;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ExternalVehicleSyncScheduler {

    private final ExternalSourceConfigStore configStore;
    private final ExternalVehicleSyncService syncService;
    private final AtomicLong lastSyncAt = new AtomicLong();

    public ExternalVehicleSyncScheduler(ExternalSourceConfigStore configStore,
                                        ExternalVehicleSyncService syncService) {
        this.configStore = configStore;
        this.syncService = syncService;
    }

    @Scheduled(fixedDelay = 1000)
    public void syncOnSchedule() {
        var config = configStore.current();
        long now = System.currentTimeMillis();
        if (!config.isEnabled()) {
            syncService.refreshStatuses();
            return;
        }
        if (now - lastSyncAt.get() < config.getPollIntervalMs()) {
            return;
        }
        if (!lastSyncAt.compareAndSet(lastSyncAt.get(), now)) {
            return;
        }
        try {
            syncService.sync();
        } catch (RuntimeException exception) {
            log.warn("外部车辆同步失败，保留已有车辆档案", exception);
            syncService.markSourceError(exception.getMessage());
            syncService.refreshStatuses();
        }
    }
}
