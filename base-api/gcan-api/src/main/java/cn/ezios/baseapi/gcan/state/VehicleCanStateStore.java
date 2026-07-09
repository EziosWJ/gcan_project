package cn.ezios.baseapi.gcan.state;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class VehicleCanStateStore {

    private final ConcurrentHashMap<Long, VehicleCanState> states = new ConcurrentHashMap<>();

    public void put(VehicleCanState state) {
        states.put(state.getVehicleId(), state);
    }

    public List<VehicleCanState> currentStates() {
        return states.values().stream()
                .sorted(Comparator.comparing(VehicleCanState::getVehicleId))
                .toList();
    }

    public VehicleCanState get(Long vehicleId) {
        return states.get(vehicleId);
    }

    public void remove(Long vehicleId) {
        states.remove(vehicleId);
    }
}
