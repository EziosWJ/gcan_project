package cn.ezios.baseapi.gcan.datagram;

import cn.ezios.baseapi.gcan.common.DatagramUtil;
import cn.ezios.baseapi.gcan.raw.RawCanFrame;
import cn.ezios.baseapi.gcan.state.VehicleCanState;
import cn.ezios.baseapi.gcan.vehicle.entity.GcanVehicle;
import java.util.List;
import java.util.Map;

public abstract class AbstractPassengerVehicleDatagramHandler extends AbstractVehicleCanDatagramHandler {

    @Override
    public VehicleCanState handle(List<RawCanFrame> frames, GcanVehicle vehicle) {
        VehicleCanState state = baseState(vehicle);
        Map<String, RawCanFrame> map = byCanId(frames);
        parse1836FF30(state, map.get("1836FF30"));
        parse043D9CF4(state, first(map, "043D9CF4", "43D9CF4"));
        parse043C9CF4(state, first(map, "043C9CF4", "43C9CF4"));
        parse1840FF30(state, map.get("1840FF30"));
        return state;
    }

    private RawCanFrame first(Map<String, RawCanFrame> map, String a, String b) {
        RawCanFrame frame = map.get(a);
        return frame == null ? map.get(b) : frame;
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
        state.setInsulationState(String.valueOf(DatagramUtil.getBit(data, 2)));
        state.setRotarySpeed(decimal(wordHL(frame, 3, 2) - 32000.00));
        state.setMotorControllerTemperature(decimal(frame.getValue(4) - 40.00));
        state.setMotorTemperature(decimal(frame.getValue(5) - 40.00));
        state.setGear(String.valueOf(frame.getValue(6)));
        state.setReadyState(String.valueOf(DatagramUtil.getBit(frame.getValue(7), 0)));
        state.setHandbrake(String.valueOf(DatagramUtil.getBit(frame.getValue(7), 1)));
        state.setBrakeActiveStatus(String.valueOf(DatagramUtil.getBit(frame.getValue(7), 2)));
        state.setLifecycle(String.valueOf(DatagramUtil.getBit(frame.getValue(7), 7)));
    }

    private void parse043D9CF4(VehicleCanState state, RawCanFrame frame) {
        if (frame == null) {
            return;
        }
        state.setBatteryVoltage(decimal(wordLH(frame, 0, 1) / 10.00));
        state.setBatteryElectric(decimal((wordLH(frame, 2, 3) - 16000) / 10.00));
        state.setBatteryPercentage(decimal(frame.getValue(4) * 0.4));
        state.setSpeed(decimal(frame.getValue(5) - 50));
        state.setFaultState(String.valueOf(wordLH(frame, 6, 7)));
    }

    private void parse043C9CF4(VehicleCanState state, RawCanFrame frame) {
        if (frame == null) {
            return;
        }
        state.setLowVoltage(decimal(wordHL(frame, 0, 1) / 1000.00));
        state.setHighVoltage(decimal(wordHL(frame, 2, 3) / 1000.00));
        state.setLowTemperature(decimal(frame.getValue(4) - 40.00));
        state.setHighTemperature(decimal(frame.getValue(5) - 40.00));
        state.setStartBatteryVoltage(decimal(wordLH(frame, 6, 7) / 10.00));
    }

    private void parse1840FF30(VehicleCanState state, RawCanFrame frame) {
        if (frame == null) {
            return;
        }
        state.setThrottleOpening(String.valueOf(decimal(frame.getValue(0) * 0.4)));
        state.setBrakePedalOpening(decimal(frame.getValue(1) * 0.4));
        state.setMotorACCurrent(decimal(wordHL(frame, 3, 2) / 10 - 1000));
        int data = frame.getValue(4);
        state.setDriveActiveStatus(String.valueOf(DatagramUtil.getBit(data, 0)));
        state.setHillStartAssistStatus(String.valueOf(DatagramUtil.getBit(data, 2)));
        state.setCreepModeStatus(String.valueOf(DatagramUtil.getBit(data, 3)));
        state.setPrechargeContactorCmd(String.valueOf(DatagramUtil.getBit(data, 4)));
        state.setMainContactorCmd(String.valueOf(DatagramUtil.getBit(data, 5)));
        state.setMotorControllerDCVoltage(decimal(frame.getValue(5) * 4));
        data = frame.getValue(7);
        state.setAccSignal(String.valueOf(DatagramUtil.getBit(data, 0)));
        state.setOnSignal(String.valueOf(DatagramUtil.getBit(data, 1)));
        state.setDriveSignal(String.valueOf(DatagramUtil.getBit(data, 2)));
        state.setReverseSignal(String.valueOf(DatagramUtil.getBit(data, 3)));
    }
}
