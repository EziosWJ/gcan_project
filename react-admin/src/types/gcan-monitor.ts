import type { GcanVehicleCanStateRecord } from "@/types/gcan";

export type GcanConnectionStatusCode = "ONLINE" | "OFFLINE" | "NO_DATA" | string;
export type GcanParseStatusCode = "SUPPORTED" | "UNSUPPORTED" | string;

export type GcanMonitorStatistics = {
  vehicleTotal: number;
  onlineCount: number;
  offlineCount: number;
  noDataCount: number;
  unsupportedCount: number;
  faultVehicleCount: number;
  latestDataAt?: string | null;
};

export type GcanMonitorFault = {
  active: boolean;
  configured: boolean;
  matched: boolean;
  code?: string | null;
  name?: string | null;
  levelCode?: string | null;
  levelName?: string | null;
  description?: string | null;
  suggestion?: string | null;
};

export type GcanMonitorVehicle = GcanVehicleCanStateRecord & {
  mineName?: string | null;
  connectionStatus?: GcanConnectionStatusCode | null;
  connectionStatusLabel?: string | null;
  parseStatus?: GcanParseStatusCode | null;
  parseStatusLabel?: string | null;
  fault?: GcanMonitorFault | null;
};

export type GcanMonitorMine = {
  mineId: string;
  mineName: string;
  sortOrder?: number | null;
  statistics: GcanMonitorStatistics;
  vehicles: GcanMonitorVehicle[];
};

export type GcanMonitorOverview = {
  statistics: GcanMonitorStatistics;
  mines: GcanMonitorMine[];
};
