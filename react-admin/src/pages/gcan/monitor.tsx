import {
  Activity,
  AlertTriangle,
  CheckCircle2,
  CircleAlert,
  Clock3,
  Maximize2,
  RefreshCw,
  ServerCrash,
  TriangleAlert,
  Wifi,
  WifiOff,
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

function StatCard({ label, value, tone, icon: Icon }: { label: string; value: number; tone: string; icon: typeof Activity }) {
  return <div className={`monitor-stat-card monitor-stat-card--${tone}`}><Icon aria-hidden="true" /><span>{label}</span><strong>{value}</strong></div>;
}

function OverviewStats({ statistics }: { statistics: GcanMonitorStatistics }) {
  return (
    <div className="monitor-stat-grid">
      <StatCard label="启用车辆" value={statistics.vehicleTotal} tone="amber" icon={Activity} />
      <StatCard label="在线" value={statistics.onlineCount} tone="cyan" icon={Wifi} />
      <StatCard label="离线" value={statistics.offlineCount} tone="slate" icon={WifiOff} />
      <StatCard label="暂无数据" value={statistics.noDataCount} tone="muted" icon={CircleAlert} />
      <StatCard label="未支持解析" value={statistics.unsupportedCount} tone="orange" icon={ServerCrash} />
      <StatCard label="当前故障" value={statistics.faultVehicleCount} tone="red" icon={TriangleAlert} />
    </div>
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
      <div className="monitor-screen__scanline" aria-hidden="true" />
      <div className="monitor-container">
        <header className="monitor-header">
          <div className="monitor-brand">
            <div className="monitor-brand__lamp" aria-hidden="true"><Activity /></div>
            <div><p>GCAN / OPEN TELEMETRY</p><h1>车辆实时监控</h1></div>
          </div>
          <div className="monitor-header__actions">
            <span className={`monitor-live-indicator ${error ? "has-error" : ""}`}><i aria-hidden="true" />{error ? "连接异常" : "实时监控中"}</span>
            <button className="monitor-icon-button" type="button" title="刷新总览" onClick={() => void loadOverview()} disabled={refreshing}><RefreshCw className={refreshing ? "is-spinning" : ""} aria-hidden="true" /></button>
            <button className="monitor-icon-button monitor-fullscreen" type="button" title="进入全屏" onClick={() => void document.documentElement.requestFullscreen?.()}><Maximize2 aria-hidden="true" /></button>
          </div>
        </header>

        <section className="monitor-hero">
          <div><span className="monitor-kicker">UNDERGROUND CONTROL ROOM</span><h2>井下车队态势 <em>/</em> 实时总览</h2><p>以启用车辆为基准 · 连接状态由最近 10 秒数据新鲜度判定</p></div>
          <div className="monitor-hero__meta"><span><Clock3 aria-hidden="true" />最近数据 <b>{formatTime(latestDataAt)}</b></span><span><RefreshCw aria-hidden="true" />轮询周期 <b>03s</b></span></div>
        </section>

        {error && <div className="monitor-error-banner"><AlertTriangle aria-hidden="true" /><span>总览刷新失败，当前展示最近一次成功数据。{overview ? "" : "请检查开放 API 服务。"}</span><b>{error}</b></div>}
        <OverviewStats statistics={statistics} />

        <section className="monitor-feed-bar">
          <div><span className="monitor-section-index">01</span><div><h2>按煤矿监控</h2><p>{activeMines.length} 个煤矿 · {statistics.vehicleTotal} 辆启用车辆</p></div></div>
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

        <footer className="monitor-footer"><span><CheckCircle2 aria-hidden="true" />匿名只读监控 · API v1</span><span><Wifi aria-hidden="true" />数据通道稳定性由开放接口提供</span></footer>
      </div>
    </main>
  );
}
