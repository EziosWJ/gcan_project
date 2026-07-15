import { http } from "@/lib/http";
import type { GcanMonitorOverview } from "@/types/gcan-monitor";

const GCAN_MONITOR_OVERVIEW_PATH = "/api/open/gcan/v1/monitor/overview";

export function getGcanMonitorOverview() {
  return http.get<GcanMonitorOverview>(GCAN_MONITOR_OVERVIEW_PATH);
}
