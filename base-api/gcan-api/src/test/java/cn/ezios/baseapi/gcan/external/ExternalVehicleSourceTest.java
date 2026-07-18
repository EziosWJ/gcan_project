package cn.ezios.baseapi.gcan.external;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.ezios.baseapi.gcan.config.ExternalSourceConfigMapper;
import cn.ezios.baseapi.gcan.config.ExternalSourceConfigStore;
import cn.ezios.baseapi.gcan.config.GcanProperties;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExternalVehicleSourceTest {

    @Test
    void loadsConfiguredMineAndVehicleEndpoints() {
        GcanProperties properties = new GcanProperties();
        properties.getExternal().setEnabled(true);
        properties.getExternal().setBaseUrl("http://external.test");
        ExternalSourceConfigStore configStore = new ExternalSourceConfigStore(properties, (ExternalSourceConfigMapper) List::of);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ExternalVehicleSource source = new ExternalVehicleSource(configStore,
                config -> builder.baseUrl(config.getBaseUrl()).build());

        server.expect(requestTo("http://external.test/api/v1/mine-config/list"))
                .andRespond(withSuccess("""
                        {"code":200,"message":"success","data":[{"mineCode":"HX","mineName":"贺西煤矿","pullFrequency":300,"enabled":1}]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://external.test/api/v1/vehicle-data/HX"))
                .andRespond(withSuccess("""
                        {"code":200,"message":"success","data":[{"vehicleCode":"R101","dataTime":"2026-07-18 10:20:30","batterySOC":82.5,"minCellVoltage":3.12,"faultCode":7}]}
                        """, MediaType.APPLICATION_JSON));

        List<ExternalMineConfig> mines = source.loadMines();
        List<ExternalVehicleData> vehicles = source.loadVehicles("HX");

        assertEquals("HX", mines.getFirst().getMineCode());
        assertEquals("贺西煤矿", mines.getFirst().getMineName());
        assertEquals("R101", vehicles.getFirst().getVehicleCode());
        assertEquals(new BigDecimal("82.5"), vehicles.getFirst().getBatterySOC());
        assertEquals(new BigDecimal("3.12"), vehicles.getFirst().getMinCellVoltage());
        assertEquals(7, vehicles.getFirst().getFaultCode());
        server.verify();
    }

}
