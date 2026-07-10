package cn.ezios.baseapi.gcan.datagram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.ezios.baseapi.gcan.raw.RawCanFrame;
import cn.ezios.baseapi.gcan.state.VehicleCanState;
import cn.ezios.baseapi.gcan.vehicle.entity.GcanVehicle;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class VehicleCanDatagramHandlerTest {

    @Test
    void materialHandlerParsesRepresentativeFrames() {
        Material19TDatagramHandler handler = new Material19TDatagramHandler();
        GcanVehicle vehicle = vehicle("LIAO_1_9T");

        VehicleCanState state = handler.handle(List.of(
                frame("08F200A0", 0x80, 0x0A, 0, 0, 0x08, 0x64, 0, 0),
                frame("1811A6A0", 0, 0x01, 0, 0, 0, 0, 0x34, 0x12),
                frame("043D9CF4", 0, 0x01, 0xF4, 0x3E, 0x80, 0x64, 0, 0)
        ), vehicle);

        assertEquals("1", state.getHandbrake());
        assertEquals("10", state.getGear());
        assertEquals("1", state.getInsulationState());
        assertDecimal("50", state.getSpeed());
        assertEquals("4660", state.getFaultState());
        assertEquals("1", state.getReadyState());
        assertDecimal("50.0", state.getBatteryVoltage());
        assertDecimal("40.0", state.getBatteryPercentage());
    }

    @Test
    void handlerMatchesVehicleTypeCaseInsensitively() {
        Material19TDatagramHandler handler = new Material19TDatagramHandler();

        assertTrue(handler.canHandle("liao_1_9t"));
    }

    @Test
    void passengerHandlerParsesRepresentativeFrames() {
        Passenger19DatagramHandler handler = new Passenger19DatagramHandler();
        GcanVehicle vehicle = vehicle("REN_19");

        VehicleCanState state = handler.handle(List.of(
                frame("1836FF30", 0x93, 0x07, 0x3D, 0x7C, 0x50, 0x51, 0x0A, 0x87),
                frame("1840FF30", 0x64, 0x32, 0x10, 0x27, 0x3D, 0x10, 0, 0x0F)
        ), vehicle);

        assertEquals("1", state.getLeftTurnLight());
        assertEquals("1", state.getRightTurnLight());
        assertEquals("1", state.getInsulationState());
        assertDecimal("-195.0", state.getRotarySpeed());
        assertDecimal("40.0", state.getMotorControllerTemperature());
        assertEquals("10", state.getGear());
        assertEquals("1", state.getLifecycle());
        assertEquals("40.0", state.getThrottleOpening());
        assertDecimal("20.0", state.getBrakePedalOpening());
        assertEquals("1", state.getDriveSignal());
        assertEquals("1", state.getReverseSignal());
    }

    private GcanVehicle vehicle(String vehicleType) {
        GcanVehicle vehicle = new GcanVehicle();
        vehicle.setId(1L);
        vehicle.setVehicleName("测试车辆");
        vehicle.setMineId("MINE_TEST");
        vehicle.setVehicleType(vehicleType);
        vehicle.setBoxIdHex("01");
        vehicle.setBoxIdDec(1);
        vehicle.setStatus(1);
        return vehicle;
    }

    private RawCanFrame frame(String canId, int... values) {
        return new RawCanFrame("01", 1, canId, values, LocalDateTime.now());
    }

    private void assertDecimal(String expected, BigDecimal actual) {
        assertTrue(new BigDecimal(expected).compareTo(actual) == 0,
                () -> "expected: <" + expected + "> but was: <" + actual + ">");
    }
}
