import type { ApiPageResult } from "@/types";
import type {
  GcanByteFormat,
  GcanRawFrameRecord,
  GcanVehicleBatchDeleteRequest,
  GcanVehicleCanStateRecord,
  GcanVehiclePageQuery,
  GcanVehicleRecord,
  GcanVehicleSaveRequest,
  GcanVehicleStatusRequest,
  GcanVehicleTypeRecord,
} from "@/types/gcan";
import { http } from "@/lib/http";

const GCAN_VEHICLE_BASE_PATH = "/api/gcan/vehicle";
const GCAN_VEHICLE_CAN_STATE_BASE_PATH = "/api/gcan/vehicle-can-state";
const GCAN_RAW_FRAME_BASE_PATH = "/api/gcan/raw-frame";

export function getGcanVehiclePage(query: GcanVehiclePageQuery) {
  return http.get<ApiPageResult<GcanVehicleRecord>>(`${GCAN_VEHICLE_BASE_PATH}/page`, {
    query,
  });
}

export function getGcanVehicleTypes() {
  return http.get<GcanVehicleTypeRecord[]>(`${GCAN_VEHICLE_BASE_PATH}/types`);
}

export function createGcanVehicle(data: GcanVehicleSaveRequest) {
  return http.post<void>(GCAN_VEHICLE_BASE_PATH, data);
}

export function updateGcanVehicle(id: number, data: GcanVehicleSaveRequest) {
  return http.put<void>(`${GCAN_VEHICLE_BASE_PATH}/${id}`, data);
}

export function deleteGcanVehicle(id: number) {
  return http.delete<void>(`${GCAN_VEHICLE_BASE_PATH}/${id}`);
}

export function updateGcanVehicleStatus(id: number, data: GcanVehicleStatusRequest) {
  return http.patch<void>(`${GCAN_VEHICLE_BASE_PATH}/${id}/status`, data);
}

export function batchDeleteGcanVehicles(data: GcanVehicleBatchDeleteRequest) {
  return http.post<void>(`${GCAN_VEHICLE_BASE_PATH}/batch-delete`, data);
}

export function getGcanVehicleCanStateCurrent() {
  return http.get<GcanVehicleCanStateRecord[]>(
    `${GCAN_VEHICLE_CAN_STATE_BASE_PATH}/current`,
  );
}

export function getGcanRawFrameCurrent(format?: GcanByteFormat) {
  return http.get<GcanRawFrameRecord[]>(`${GCAN_RAW_FRAME_BASE_PATH}/current`, {
    query: format ? { format } : undefined,
  });
}

export async function getGcanRawFrameCurrentTable(format?: GcanByteFormat) {
  const blob = await http.blob(`${GCAN_RAW_FRAME_BASE_PATH}/current/table`, {
    query: format ? { format } : undefined,
    headers: {
      Accept: "text/plain",
    },
  });
  return blob.text();
}
