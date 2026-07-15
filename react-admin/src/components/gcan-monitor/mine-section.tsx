import { ChevronDown, ChevronRight, Pickaxe } from "lucide-react";
import type { GcanMonitorMine } from "@/types/gcan-monitor";
import { VehicleCard } from "@/components/gcan-monitor/vehicle-card";

type MineSectionProps = {
  mine: GcanMonitorMine;
  open: boolean;
  onToggle: () => void;
};

const statItems = [
  { key: "vehicleTotal", label: "车辆" },
  { key: "onlineCount", label: "在线" },
  { key: "offlineCount", label: "离线" },
  { key: "noDataCount", label: "暂无数据" },
  { key: "unsupportedCount", label: "未解析" },
  { key: "faultVehicleCount", label: "故障" },
] as const;

export function MineSection({ mine, open, onToggle }: MineSectionProps) {
  return (
    <section className={`monitor-mine ${open ? "is-open" : "is-collapsed"}`}>
      <button className="monitor-mine__heading" type="button" aria-expanded={open} onClick={onToggle}>
        <span className="monitor-mine__title">
          <span className="monitor-mine__icon"><Pickaxe aria-hidden="true" /></span>
          <span><strong>{mine.mineName}</strong><small>{mine.mineId}</small></span>
        </span>
        <span className="monitor-mine__stats">
          {statItems.map((item) => (
            <span key={item.key}><b>{mine.statistics[item.key]}</b>{item.label}</span>
          ))}
        </span>
        <span className="monitor-mine__chevron">{open ? <ChevronDown aria-hidden="true" /> : <ChevronRight aria-hidden="true" />}</span>
      </button>
      {open && (
        <div className="monitor-vehicle-grid">
          {mine.vehicles.map((vehicle) => <VehicleCard key={vehicle.vehicleId} vehicle={vehicle} />)}
        </div>
      )}
    </section>
  );
}
