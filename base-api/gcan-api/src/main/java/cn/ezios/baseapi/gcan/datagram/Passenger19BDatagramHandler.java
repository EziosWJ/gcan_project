package cn.ezios.baseapi.gcan.datagram;

import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class Passenger19BDatagramHandler extends AbstractPassengerVehicleDatagramHandler {

    @Override
    public Set<String> supportedVehicleTypes() {
        return Set.of("REN_19_B");
    }
}
