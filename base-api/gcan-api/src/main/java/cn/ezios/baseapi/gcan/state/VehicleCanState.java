package cn.ezios.baseapi.gcan.state;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class VehicleCanState {

    private Long vehicleId;
    private String vehicleName;
    private String mineId;
    private String accessMode;
    private String externalVehicleCode;
    private String vehicleType;
    private String vehicleTypeLabel;
    private String boxIdHex;
    private Integer boxIdDec;
    private Boolean online;
    private Boolean parseSupported = true;
    private String parseMessage;
    private String connectionStatus = "NO_DATA";
    private String parseStatus = "SUPPORTED";
    private Boolean sourceError;
    private String sourceErrorMessage;
    private LocalDateTime lastReceivedAt;
    private LocalDateTime updateTime;

    private BigDecimal highVoltage;
    private BigDecimal lowVoltage;
    private BigDecimal highTemperature;
    private BigDecimal lowTemperature;
    private BigDecimal motorControllerTemperature;
    private BigDecimal motorTemperature;
    private String insulationState;
    private BigDecimal startBatteryVoltage;
    private BigDecimal rotarySpeed;
    private String faultState = "0";
    private String throttleOpening;
    private BigDecimal batteryPercentage;
    private String handbrake;
    private BigDecimal batteryVoltage;
    private BigDecimal batteryElectric;
    private BigDecimal speed;
    private BigDecimal totalMileage;
    private String runState;
    private String gear;
    private String lifecycle;
    private BigDecimal brakePedalOpening;
    private BigDecimal motorACCurrent;
    private String driveActiveStatus;
    private String brakeActiveStatus;
    private String hillStartAssistStatus;
    private String creepModeStatus;
    private String prechargeContactorCmd;
    private String mainContactorCmd;
    private BigDecimal motorControllerDCVoltage;
    private String accSignal;
    private String onSignal;
    private String driveSignal;
    private String reverseSignal;
    private String leftTurnLight;
    private String rightTurnLight;
    private String highBeam;
    private String lowBeam;
    private String smallLight;
    private String door1Open;
    private String door2Open;
    private String door3Open;
    private BigDecimal mcuTemperature;
    private String methaneDetectionFailure;
    private String smokeDetectionFailure;
    private String readyState;
}
