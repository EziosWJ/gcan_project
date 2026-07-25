import {
  AlertTriangle,
  ChevronDown,
  ChevronRight,
  Cpu,
  Zap,
} from "lucide-react";
import { useState } from "react";
import { MetricBar } from "@/components/gcan-monitor/metric-bar";
import type {
  GcanConnectionStatusCode,
  GcanMonitorFault,
  GcanMonitorVehicle,
  GcanParseStatusCode,
} from "@/types/gcan-monitor";

type VehicleCardProps = {
  vehicle: GcanMonitorVehicle;
};

type DetailItem = {
  label: string;
  value: number | string | null | undefined;
};

function valueOrDash(value: DetailItem["value"]) {
  if (value === null || value === undefined || value === "") return "—";
  return String(value);
}

function detailValue(value: DetailItem["value"]) {
  if (value === 0 || value === "0") return "关闭";
  if (value === 1 || value === "1") return "打开";
  return valueOrDash(value);
}

function nonNegativeValue(value: DetailItem["value"]) {
  if (value === null || value === undefined || value === "") return value;
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? Math.max(0, numericValue) : value;
}

function gearLabel(value: DetailItem["value"]) {
  if (value === null || value === undefined || value === "") return "—";
  return { "0": "N", "1": "P", "5": "R", "10": "D", "255": "故障" }[String(value)] ?? String(value);
}

function valueWithUnit(value: DetailItem["value"], unit: string) {
  return value === null || value === undefined || value === "" ? null : `${value} ${unit}`;
}

function getConnectionCode(vehicle: GcanMonitorVehicle): GcanConnectionStatusCode {
  if (vehicle.connectionStatus) return vehicle.connectionStatus;
  if (vehicle.online === true) return "ONLINE";
  return vehicle.lastReceivedAt ? "OFFLINE" : "NO_DATA";
}

function getParseCode(vehicle: GcanMonitorVehicle): GcanParseStatusCode {
  if (vehicle.parseStatus) return vehicle.parseStatus;
  return vehicle.parseSupported === false ? "UNSUPPORTED" : "SUPPORTED";
}

function connectionLabel(vehicle: GcanMonitorVehicle, code: string) {
  if (vehicle.connectionStatusLabel) return vehicle.connectionStatusLabel;
  return { ONLINE: "在线", OFFLINE: "离线", NO_DATA: "暂无数据" }[code] ?? code;
}

function parseLabel(vehicle: GcanMonitorVehicle, code: string) {
  if (vehicle.parseStatusLabel) return vehicle.parseStatusLabel;
  return { SUPPORTED: "已支持解析", UNSUPPORTED: "未支持解析" }[code] ?? code;
}

function connectionTone(code: string) {
  if (code === "ONLINE") return "online";
  if (code === "OFFLINE") return "offline";
  return "no-data";
}

function getFaultPresentation(
  vehicle: GcanMonitorVehicle,
  fault: GcanMonitorFault | null | undefined,
  connectionCode: string,
  parseCode: string,
) {
  if (connectionCode === "NO_DATA" || parseCode === "UNSUPPORTED") {
    return { label: "状态不可用", detail: connectionCode === "NO_DATA" ? "暂无数据" : "未支持解析", tone: "warning" };
  }

  if (fault?.active) {
    if (fault.matched && fault.name) {
      return { label: fault.name, detail: fault.code ?? "FAULT", tone: "fault" };
    }
    if (!fault.configured) {
      return { label: "未配置故障码表", detail: fault.code ?? "故障", tone: "warning" };
    }
    return { label: "未知故障码", detail: fault.code ?? "故障", tone: "warning" };
  }

  if (vehicle.faultState && vehicle.faultState !== "0") {
    return { label: "未知故障码", detail: vehicle.faultState, tone: "warning" };
  }

  return { label: "无活动故障", detail: "正常", tone: "normal" };
}

function detailGroups(vehicle: GcanMonitorVehicle): { title: string; items: DetailItem[] }[] {
  const faultItems = vehicle.fault?.active
    ? [
        { label: "故障码", value: vehicle.fault.code },
        { label: "故障等级", value: vehicle.fault.levelName ?? vehicle.fault.levelCode },
        { label: "故障说明", value: vehicle.fault.description },
        { label: "处理建议", value: vehicle.fault.suggestion },
      ]
    : [];
  return [
    ...(faultItems.length > 0 ? [{ title: "故障详情", items: faultItems }] : []),
    {
      title: "控制状态",
      items: [
        { label: "生命周期", value: vehicle.lifecycle },
        { label: "绝缘状态", value: vehicle.insulationState },
        { label: "手刹", value: vehicle.handbrake },
        { label: "刹车踏板", value: vehicle.brakePedalOpening },
        { label: "准备状态", value: vehicle.readyState },
        { label: "续航里程", value: valueWithUnit(vehicle.totalMileage, "km") },
      ],
    },
    {
      title: "灯光 / 车门",
      items: [
        { label: "左转灯", value: vehicle.leftTurnLight },
        { label: "右转灯", value: vehicle.rightTurnLight },
        { label: "远光灯", value: vehicle.highBeam },
        { label: "近光灯", value: vehicle.lowBeam },
        { label: "小灯", value: vehicle.smallLight },
        { label: "车门 1 / 2 / 3", value: [vehicle.door1Open, vehicle.door2Open, vehicle.door3Open].map(detailValue).join(" / ") },
      ],
    },
    {
      title: "传感器 / 执行器",
      items: [
        { label: "MCU 温度", value: valueWithUnit(vehicle.mcuTemperature, "℃") },
        { label: "驱动激活", value: vehicle.driveActiveStatus },
        { label: "制动激活", value: vehicle.brakeActiveStatus },
        { label: "爬行模式", value: vehicle.creepModeStatus },
        { label: "甲烷检测", value: vehicle.methaneDetectionFailure },
        { label: "烟雾检测", value: vehicle.smokeDetectionFailure },
      ],
    },
  ];
}

export function VehicleCard({ vehicle }: VehicleCardProps) {
  const [detailsOpen, setDetailsOpen] = useState(false);
  const connectionCode = getConnectionCode(vehicle);
  const parseCode = getParseCode(vehicle);
  const faultPresentation = getFaultPresentation(vehicle, vehicle.fault, connectionCode, parseCode);
  const hasData = connectionCode !== "NO_DATA";
  const groups = detailGroups(vehicle);

  return (
    <article className={`monitor-vehicle-card monitor-vehicle-card--${connectionTone(connectionCode)}`}>
      <div className="monitor-vehicle-card__header">
        <div className="monitor-vehicle-card__identity">
          <div className="monitor-vehicle-card__signal" aria-hidden="true">
            {connectionCode === "ONLINE" ? <Zap /> : <Cpu />}
          </div>
          <div>
            <h3>{vehicle.vehicleName}</h3>
            <p>
              {vehicle.vehicleTypeLabel ?? vehicle.vehicleType}
              <span>·</span>
              {vehicle.accessMode === "MINE_API" ? (vehicle.externalVehicleCode ?? "外部车辆") : (vehicle.boxIdHex ?? "-")}
            </p>
          </div>
        </div>
        <span className={`monitor-status monitor-status--${connectionTone(connectionCode)}`}>
          <i aria-hidden="true" />
          {connectionLabel(vehicle, connectionCode)}
        </span>
      </div>

      <div className="monitor-vehicle-card__status-row">
        <span className={`monitor-parse-status monitor-parse-status--${parseCode === "SUPPORTED" ? "supported" : "unsupported"}`}>
          <span>{parseLabel(vehicle, parseCode)}</span>
        </span>
        <span className="monitor-vehicle-card__received">
          最近接收 {vehicle.lastReceivedAt ? vehicle.lastReceivedAt.replace("T", " ").slice(0, 19) : "—"}
        </span>
      </div>

      <div className="monitor-vehicle-card__metrics">
        <MetricBar label="车速" value={nonNegativeValue(vehicle.speed)} unit="km/h" min={0} max={40} />
        <MetricBar label="电量" value={vehicle.batteryPercentage} unit="%" min={0} max={100} precision={2} />
        <MetricBar label="电池电压" value={vehicle.batteryVoltage} unit="V" min={0} max={1000} />
        <MetricBar label="电流" value={vehicle.motorACCurrent ?? vehicle.batteryElectric} unit="A" min={-300} max={300} signed />
        <MetricBar label="高压" value={vehicle.highVoltage} unit="V" min={0} max={1000} />
        <MetricBar label="低压" value={vehicle.lowVoltage} unit="V" min={0} max={32} />
        <MetricBar label="高温" value={vehicle.highTemperature} unit="℃" min={-40} max={150} />
        <MetricBar label="低温" value={vehicle.lowTemperature} unit="℃" min={-40} max={150} />
      </div>

      <div className="monitor-vehicle-card__quick-row">
        <div><span>运行状态</span><strong>{hasData ? valueOrDash(vehicle.runState) : "暂无数据"}</strong></div>
        <div><span>档位</span><strong>{hasData ? gearLabel(vehicle.gear) : "—"}</strong></div>
        <div className={`monitor-fault monitor-fault--${faultPresentation.tone}`}>
          <AlertTriangle aria-hidden="true" /><span>{faultPresentation.label}</span><b>{faultPresentation.detail}</b>
        </div>
      </div>

      <button
        className="monitor-vehicle-card__details-toggle"
        type="button"
        aria-expanded={detailsOpen}
        onClick={() => setDetailsOpen((open) => !open)}
      >
        {detailsOpen ? <ChevronDown aria-hidden="true" /> : <ChevronRight aria-hidden="true" />}
        {detailsOpen ? "收起实时详情" : "展开控制、灯光与传感器详情"}
      </button>

      {detailsOpen && (
        <div className="monitor-vehicle-card__details">
          {groups.map((group) => (
            <div key={group.title} className="monitor-detail-group">
              <h4>{group.title}</h4>
              <dl>
                {group.items.map((item) => (
                  <div key={item.label}><dt>{item.label}</dt><dd>{detailValue(item.value)}</dd></div>
                ))}
              </dl>
            </div>
          ))}
        </div>
      )}
    </article>
  );
}
