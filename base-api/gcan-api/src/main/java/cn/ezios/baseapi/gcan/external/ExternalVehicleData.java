package cn.ezios.baseapi.gcan.external;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class ExternalVehicleData {

    private String vehicleCode;
    private String dataTime;
    private Integer handbrakeStatus;
    private Integer insulationAlarm;
    private Integer leftTurnSignal;
    private Integer rightTurnSignal;
    private Integer highBeam;
    private Integer lowBeam;
    private Integer parkingLight;
    private Integer door1Open;
    private Integer methaneFault;
    private Integer smokeFault;
    private BigDecimal motorSpeed;
    private BigDecimal controllerTemperature;
    private BigDecimal motorTemperature;
    private String gearPosition;
    private Integer readyIndicator;
    private String lifecycle;
    private Integer door2Open;
    private Integer door3Open;
    private BigDecimal acceleratorPedalOpening;
    private BigDecimal brakePedalOpening;
    private BigDecimal motorACCurrent;
    private Integer driveActiveStatus;
    private Integer brakeActiveStatus;
    private Integer hillStartAssistStatus;
    private Integer creepModeStatus;
    private Integer prechargeContactorCmd;
    private Integer mainContactorCmd;
    private BigDecimal motorControllerDCVoltage;
    private Integer accSignal;
    private Integer onSignal;
    private Integer driveSignal;
    private Integer reverseSignal;
    private BigDecimal mcuTemperature;
    private BigDecimal minCellVoltage;
    private BigDecimal maxCellVoltage;
    private BigDecimal minModuleTemp;
    private BigDecimal maxModuleTemp;
    private BigDecimal auxiliaryBatteryVoltage;
    private BigDecimal voltage;
    private BigDecimal electricity;
    private BigDecimal batterySOC;
    private BigDecimal speed;
    private Integer faultCode;
}
