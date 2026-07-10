import type { ApiPageResult } from "@/types";
import type {
  GcanByteFormat,
  GcanRawFrameHistoryQuery,
  GcanRawFrameQuery,
  GcanRawFrameRecord,
  GcanVehicleBatchDeleteRequest,
  GcanVehicleCanStateRecord,
  GcanVehicleCanStateQuery,
  GcanVehiclePageQuery,
  GcanVehicleRecord,
  GcanVehicleSaveRequest,
  GcanVehicleStatusRequest,
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

export function getGcanVehicleCanStateCurrent(query?: GcanVehicleCanStateQuery) {
  return http.get<GcanVehicleCanStateRecord[]>(
    `${GCAN_VEHICLE_CAN_STATE_BASE_PATH}/current`,
    {
      query,
    },
  );
}

export function getGcanRawFrameCurrent(query?: GcanRawFrameQuery, format?: GcanByteFormat) {
  return http.get<GcanRawFrameRecord[]>(`${GCAN_RAW_FRAME_BASE_PATH}/current`, {
    query: {
      ...query,
      format,
    },
  });
}

export async function getGcanRawFrameCurrentTable(query?: GcanRawFrameQuery, format?: GcanByteFormat) {
  const blob = await http.blob(`${GCAN_RAW_FRAME_BASE_PATH}/current/table`, {
    query: {
      ...query,
      format,
    },
    headers: {
      Accept: "text/plain",
    },
  });
  return blob.text();
}

export function getGcanRawFrameHistoryPage(query: GcanRawFrameHistoryQuery, format?: GcanByteFormat) {
  return http.get<ApiPageResult<GcanRawFrameRecord>>(
    `${GCAN_RAW_FRAME_BASE_PATH}/history/page`,
    {
      query: {
        ...query,
        format,
      },
    },
  );
}
