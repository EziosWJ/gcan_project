import type { ApiPageRequest, ApiStatus } from "./api";

export type GcanVehicleTypeRecord = {
  code: string;
  label: string;
};

export type GcanVehicleRecord = {
  id: number;
  vehicleName: string;
  vehicleType: string;
  vehicleTypeLabel?: string;
  boxIdHex: string;
  boxIdDec: number;
  status: ApiStatus;
  remark?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
};

export type GcanVehiclePageQuery = Partial<ApiPageRequest> & {
  vehicleName?: string;
  vehicleType?: string;
  boxIdHex?: string;
  status?: ApiStatus;
};

export type GcanVehicleSaveRequest = {
  vehicleName: string;
  vehicleType: string;
  boxIdHex: string;
  status?: ApiStatus;
  remark?: string;
};

export type GcanVehicleStatusRequest = {
  status: ApiStatus;
};

export type GcanVehicleBatchDeleteRequest = {
  ids: number[];
};

export type GcanVehicleCanStateRecord = {
  vehicleId: number;
  vehicleName: string;
  vehicleType: string;
  vehicleTypeLabel?: string;
  boxIdHex: string;
  boxIdDec: number;
  online: boolean;
  lastReceivedAt?: string | null;
  updateTime?: string | null;
  highVoltage?: number | string | null;
  lowVoltage?: number | string | null;
  highTemperature?: number | string | null;
  lowTemperature?: number | string | null;
  motorControllerTemperature?: number | string | null;
  motorTemperature?: number | string | null;
  insulationState?: string | null;
  startBatteryVoltage?: number | string | null;
  rotarySpeed?: number | string | null;
  faultState?: string | null;
  throttleOpening?: string | null;
  batteryPercentage?: number | string | null;
  handbrake?: string | null;
  batteryVoltage?: number | string | null;
  batteryElectric?: number | string | null;
  speed?: number | string | null;
  totalMileage?: number | string | null;
  runState?: string | null;
  gear?: string | null;
  lifecycle?: string | null;
  brakePedalOpening?: number | string | null;
  motorACCurrent?: number | string | null;
  driveActiveStatus?: string | null;
  brakeActiveStatus?: string | null;
  hillStartAssistStatus?: string | null;
  creepModeStatus?: string | null;
  prechargeContactorCmd?: string | null;
  mainContactorCmd?: string | null;
  motorControllerDCVoltage?: number | string | null;
  accSignal?: string | null;
  onSignal?: string | null;
  driveSignal?: string | null;
  reverseSignal?: string | null;
  leftTurnLight?: string | null;
  rightTurnLight?: string | null;
  highBeam?: string | null;
  lowBeam?: string | null;
  smallLight?: string | null;
  door1Open?: string | null;
  door2Open?: string | null;
  door3Open?: string | null;
  mcuTemperature?: number | string | null;
  methaneDetectionFailure?: string | null;
  smokeDetectionFailure?: string | null;
  readyState?: string | null;
};

export type GcanByteFormat = "hex" | "bin" | "decimal";

export type GcanRawFrameRecord = {
  boxIdHex: string;
  boxIdDec: number;
  canId: string;
  data: string[];
  receivedAt?: string | null;
};

