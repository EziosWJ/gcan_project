package cn.ezios.baseapi.gcan.datagram;

import cn.ezios.baseapi.gcan.common.DatagramUtil;
import cn.ezios.baseapi.gcan.raw.RawCanFrame;
import cn.ezios.baseapi.gcan.state.VehicleCanState;
import cn.ezios.baseapi.gcan.vehicle.entity.GcanVehicle;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public abstract class AbstractMaterialVehicleDatagramHandler extends AbstractVehicleCanDatagramHandler {

    @Override
    public VehicleCanState handle(List<RawCanFrame> frames, GcanVehicle vehicle) {
        VehicleCanState state = baseState(vehicle);
        Map<String, RawCanFrame> map = byCanId(frames);
        parse08F200A0(state, map.get("08F200A0"));
        parse1811A6A0(state, map.get("1811A6A0"));
        parse1836FF30(state, map.get("1836FF30"));
        parse0C10A7F0(state, map.get("0C10A7F0"));
        parse0C12A7F0(state, map.get("0C12A7F0"));
        parse043D9CF4(state, map.get("043D9CF4"));
        parse043C9CF4(state, map.get("043C9CF4"));
        parse1830D02C(state, map.get("1830D02C"));
        return state;
    }

    private void parse08F200A0(VehicleCanState state, RawCanFrame frame) {
        if (frame == null) {
            return;
        }
        state.setHandbrake(String.valueOf(DatagramUtil.getBit(frame.getValue(0), 7)));
        state.setGear(String.valueOf(frame.getValue(1)));
        state.setInsulationState(String.valueOf(DatagramUtil.getBit(frame.getValue(4), 3)));
        state.setSpeed(decimal(frame.getValue(5) - 50));
    }

    private void parse1811A6A0(VehicleCanState state, RawCanFrame frame) {
        if (frame == null) {
            return;
        }
        state.setFaultState(String.valueOf(wordLH(frame, 6, 7)));
        state.setReadyState(String.valueOf(DatagramUtil.getBit(frame.getValue(1), 0)));
    }

    private void parse1836FF30(VehicleCanState state, RawCanFrame frame) {
        if (frame == null) {
            return;
        }
        int data = frame.getValue(0);
        state.setLeftTurnLight(String.valueOf(DatagramUtil.getBit(data, 0)));
        state.setRightTurnLight(String.valueOf(DatagramUtil.getBit(data, 1)));
        state.setHighBeam(String.valueOf(DatagramUtil.getBit(data, 2)));
        state.setLowBeam(String.valueOf(DatagramUtil.getBit(data, 3)));
        state.setSmallLight(String.valueOf(DatagramUtil.getBit(data, 4)));
        state.setDoor1Open(String.valueOf(DatagramUtil.getBit(data, 7)));

        data = frame.getValue(1);
        state.setMethaneDetectionFailure(String.valueOf(DatagramUtil.getBit(data, 0)));
        state.setSmokeDetectionFailure(String.valueOf(DatagramUtil.getBit(data, 1)));
        state.setDoor2Open(String.valueOf(DatagramUtil.getBit(data, 4)));
        state.setDoor3Open(String.valueOf(DatagramUtil.getBit(data, 5)));
        state.setThrottleOpening(String.valueOf(decimal(frame.getValue(4) * 0.4)));
        state.setBrakeActiveStatus(String.valueOf(DatagramUtil.getBit(frame.getValue(5), 4) * 2
                + DatagramUtil.getBit(frame.getValue(5), 5)));
    }

    private void parse0C10A7F0(VehicleCanState state, RawCanFrame frame) {
        if (frame != null) {
            state.setRotarySpeed(decimal(wordHL(frame, 3, 2) - 32000.00));
        }
    }

    private void parse0C12A7F0(VehicleCanState state, RawCanFrame frame) {
        if (frame == null) {
            return;
        }
        state.setMotorControllerTemperature(decimal(frame.getValue(6) - 40));
        state.setMotorTemperature(decimal(frame.getValue(7) - 40));
    }

    private void parse043D9CF4(VehicleCanState state, RawCanFrame frame) {
        if (frame == null) {
            return;
        }
        state.setBatteryVoltage(decimal((frame.getValue(2) + frame.getValue(1) * 256) / 10.00));
        state.setBatteryElectric(decimal((frame.getValue(4) + frame.getValue(3) * 256 - 16000) / 10.00));
        state.setBatteryPercentage(decimal(frame.getValue(5) * 0.4));
    }

    private void parse043C9CF4(VehicleCanState state, RawCanFrame frame) {
        if (frame == null) {
            return;
        }
        state.setLowVoltage(decimal((frame.getValue(1) + frame.getValue(0) * 256) / 1000.00));
        state.setHighVoltage(decimal((frame.getValue(3) + frame.getValue(2) * 256) / 1000.00));
        state.setLowTemperature(decimal(frame.getValue(4) - 40.00));
        state.setHighTemperature(decimal(frame.getValue(5) - 40.00));
    }

    private void parse1830D02C(VehicleCanState state, RawCanFrame frame) {
        if (frame != null) {
            state.setStartBatteryVoltage(BigDecimal.valueOf(wordLH(frame, 0, 1) / 10.00));
        }
    }
}
