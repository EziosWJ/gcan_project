package cn.ezios.baseapi.gcan.status;

import cn.ezios.baseapi.gcan.config.GcanProperties;
import cn.ezios.baseapi.gcan.datagram.VehicleCanDatagramHandler;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class VehicleStatusResolver {

    private final List<VehicleCanDatagramHandler> handlers;
    private final GcanProperties properties;

    public VehicleStatusResolver(List<VehicleCanDatagramHandler> handlers, GcanProperties properties) {
        this.handlers = handlers;
        this.properties = properties;
    }

    public VehicleStatus resolve(String vehicleType, LocalDateTime latestReceivedAt) {
        return new VehicleStatus(
                resolveConnectionStatus(latestReceivedAt),
                resolveParseStatus(vehicleType));
    }

    public VehicleConnectionStatus resolveConnectionStatus(LocalDateTime latestReceivedAt) {
        if (latestReceivedAt == null) {
            return VehicleConnectionStatus.NO_DATA;
        }
        return Duration.between(latestReceivedAt, LocalDateTime.now()).toMillis()
                <= properties.getFrameStaleThresholdMs()
                ? VehicleConnectionStatus.ONLINE
                : VehicleConnectionStatus.OFFLINE;
    }

    public VehicleParseStatus resolveParseStatus(String vehicleType) {
        return handlers.stream().anyMatch(handler -> handler.canHandle(vehicleType))
                ? VehicleParseStatus.SUPPORTED
                : VehicleParseStatus.UNSUPPORTED;
    }
}
