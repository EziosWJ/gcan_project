package cn.ezios.baseapi.gcan.datagram;

import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class Material19TDatagramHandler extends AbstractMaterialVehicleDatagramHandler {

    @Override
    public Set<String> supportedVehicleTypes() {
        return Set.of("LIAO_1_9T");
    }
}
