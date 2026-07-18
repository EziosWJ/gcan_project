import {
  Activity,
  AlertTriangle,
  CheckCircle2,
  CircleAlert,
  Clock3,
  Gauge,
  Maximize2,
  RefreshCw,
  TriangleAlert,
  Wifi,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { getGcanMonitorOverview } from "@/api/gcan-monitor";
import { MineSection } from "@/components/gcan-monitor/mine-section";
import { getErrorMessage } from "@/lib/api-error";
import type { GcanMonitorOverview, GcanMonitorStatistics } from "@/types/gcan-monitor";
import "@/styles/gcan-monitor.css";

const POLL_INTERVAL = 3000;

const EMPTY_STATISTICS: GcanMonitorStatistics = {
  vehicleTotal: 0,
  onlineCount: 0,
  offlineCount: 0,
  noDataCount: 0,
  unsupportedCount: 0,
  faultVehicleCount: 0,
  latestDataAt: null,
};

function formatTime(value?: string | null) {
  if (!value) return "—";
  return value.replace("T", " ").replace(/\.\d{1,6}/, "").slice(0, 19);
}

function formatLocalTime(value: Date | null) {
  return value ? value.toLocaleTimeString("zh-CN", { hour12: false }) : "—";
}

function OverviewStats({ statistics }: { statistics: GcanMonitorStatistics }) {
  const total = statistics.vehicleTotal;
  const onlineRate = total > 0 ? Math.round((statistics.onlineCount / total) * 100) : 0;
  const statusItems = [
    { label: "在线", value: statistics.onlineCount, tone: "cyan" },
    { label: "离线", value: statistics.offlineCount, tone: "slate" },
    { label: "暂无数据", value: statistics.noDataCount, tone: "amber" },
    { label: "未支持解析", value: statistics.unsupportedCount, tone: "orange" },
  ];

  return (
    <section className="monitor-overview-grid" aria-label="车辆状态统计">
      <div className="monitor-live-readout">
        <div className="monitor-panel-heading">
          <span>全场车辆</span>
          <Gauge aria-hidden="true" />
        </div>
        <div className="monitor-live-readout__body">
          <div>
            <strong>{total}</strong>
            <span>辆启用车辆</span>
          </div>
          <div
            className="monitor-status-ring"
            style={{
              background: `conic-gradient(var(--monitor-cyan) ${onlineRate}%, rgba(111, 132, 116, .16) 0)`,
            }}
          >
            <div>
              <strong>{onlineRate}%</strong>
              <span>在线率</span>
            </div>
          </div>
        </div>
        <div className="monitor-live-readout__footer">
          <span><i className="is-cyan" />在线 {statistics.onlineCount}</span>
          <span><i className="is-red" />故障 {statistics.faultVehicleCount}</span>
        </div>
      </div>

      <div className="monitor-distribution">
        <div className="monitor-panel-heading">
          <div><span>连接状态</span><small>当前车辆分布</small></div>
          <span className="monitor-panel-heading__total">{total} 辆</span>
        </div>
        <div className="monitor-distribution__list">
          {statusItems.map((item) => {
            const percentage = total > 0 ? Math.round((item.value / total) * 100) : 0;
            return (
              <div className="monitor-distribution__item" key={item.label}>
                <div><span>{item.label}</span><b>{item.value}<small>{percentage}%</small></b></div>
                <div className="monitor-distribution__track"><i className={`is-${item.tone}`} style={{ width: `${percentage}%` }} /></div>
              </div>
            );
          })}
        </div>
      </div>

      <div className="monitor-alert-summary">
        <div className="monitor-panel-heading">
          <span>值守摘要</span>
          <span className="monitor-alert-summary__live"><i />实时</span>
        </div>
        <div className="monitor-alert-summary__main">
          <TriangleAlert aria-hidden="true" />
          <div><strong>{statistics.faultVehicleCount}</strong><span>辆当前故障</span></div>
        </div>
        <div className="monitor-alert-summary__rows">
          <span>暂无数据 <b>{statistics.noDataCount}</b></span>
          <span>未支持解析 <b>{statistics.unsupportedCount}</b></span>
          <span>离线车辆 <b>{statistics.offlineCount}</b></span>
        </div>
      </div>
    </section>
  );
}

export function GcanMonitorPage() {
  const [overview, setOverview] = useState<GcanMonitorOverview | null>(null);
  const [collapsedMineIds, setCollapsedMineIds] = useState<Set<string>>(() => new Set());
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");
  const [lastSuccessAt, setLastSuccessAt] = useState<Date | null>(null);

  const loadOverview = useCallback(async () => {
    setRefreshing(true);
    try {
      const data = await getGcanMonitorOverview();
      setOverview(data);
      setError("");
      setLastSuccessAt(new Date());
    } catch (loadError) {
      setError(getErrorMessage(loadError, "监控总览连接失败"));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    document.title = "GCAN · 独立监控大屏";
    void loadOverview();
    const timer = window.setInterval(() => void loadOverview(), POLL_INTERVAL);
    return () => window.clearInterval(timer);
  }, [loadOverview]);

  const statistics = overview?.statistics ?? EMPTY_STATISTICS;
  const latestDataAt = statistics.latestDataAt;
  const activeMines = useMemo(() => overview?.mines ?? [], [overview]);

  return (
    <main className="monitor-screen">
      <div className="monitor-container">
        <header className="monitor-header">
          <div className="monitor-brand">
            <div className="monitor-brand__lamp" aria-hidden="true"><Activity /></div>
            <div><h1>车辆实时监控</h1><span>GCAN 车队状态</span></div>
          </div>
          <div className="monitor-header__readout"><strong>{statistics.onlineCount}<small> / {statistics.vehicleTotal}</small></strong><span>在线车辆</span></div>
          <div className="monitor-header__actions">
            <span className={`monitor-live-indicator ${error ? "has-error" : ""}`}><i aria-hidden="true" />{error ? "连接异常" : "实时监控中"}</span>
            <span className="monitor-header__time">{formatLocalTime(lastSuccessAt)}</span>
            <button className="monitor-icon-button" type="button" title="刷新总览" onClick={() => void loadOverview()} disabled={refreshing}><RefreshCw className={refreshing ? "is-spinning" : ""} aria-hidden="true" /></button>
            <button className="monitor-icon-button monitor-fullscreen" type="button" title="进入全屏" onClick={() => void document.documentElement.requestFullscreen?.()}><Maximize2 aria-hidden="true" /></button>
          </div>
        </header>

        <section className="monitor-command-bar">
          <div><span>当前态势</span><strong>井下车队运行状态</strong></div>
          <div className="monitor-command-bar__meta"><span><Clock3 aria-hidden="true" />最新数据 <b>{formatTime(latestDataAt)}</b></span><span><RefreshCw aria-hidden="true" />自动刷新 <b>每 3 秒</b></span></div>
        </section>

        {error && <div className="monitor-error-banner"><AlertTriangle aria-hidden="true" /><span>总览刷新失败，当前展示最近一次成功数据。{overview ? "" : "请检查开放 API 服务。"}</span><b>{error}</b></div>}
        <OverviewStats statistics={statistics} />

        <section className="monitor-feed-bar">
          <div><div><h2>煤矿车辆</h2><p>{activeMines.length} 个煤矿 · {statistics.vehicleTotal} 辆启用车辆</p></div></div>
          <div className="monitor-feed-bar__legend"><span><i className="is-cyan" />在线</span><span><i className="is-amber" />待关注</span><span><i className="is-red" />故障</span><small>上次成功 {formatLocalTime(lastSuccessAt)}</small></div>
        </section>

        {loading && !overview ? (
          <div className="monitor-loading"><RefreshCw className="is-spinning" aria-hidden="true" /><strong>正在接入井下遥测...</strong><span>等待监控总览 v1 返回</span></div>
        ) : activeMines.length > 0 ? (
          <div className="monitor-mine-list">
            {activeMines.map((mine) => <MineSection key={mine.mineId} mine={mine} open={!collapsedMineIds.has(mine.mineId)} onToggle={() => setCollapsedMineIds((current) => { const next = new Set(current); if (next.has(mine.mineId)) next.delete(mine.mineId); else next.add(mine.mineId); return next; })} />)}
          </div>
        ) : (
          <div className="monitor-empty"><CircleAlert aria-hidden="true" /><strong>暂无启用车辆</strong><span>监控总览暂未返回可展示的煤矿与车辆。</span></div>
        )}

        <footer className="monitor-footer"><span><CheckCircle2 aria-hidden="true" />只读监控 · 数据正常接入</span><span><Wifi aria-hidden="true" />每 3 秒自动刷新</span></footer>
      </div>
    </main>
  );
}
