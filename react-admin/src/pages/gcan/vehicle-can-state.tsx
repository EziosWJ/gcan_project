import { RefreshCw, RotateCcw, Search } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { getGcanVehicleCanStateCurrent } from "@/api/gcan";
import { DataTable } from "@/components/common/data-table";
import { EmptyState } from "@/components/common/empty-state";
import { PageHeader } from "@/components/common/page-header";
import { SearchFilterBar } from "@/components/common/search-filter-bar";
import { TableToolbar } from "@/components/common/table-toolbar";
import { toast } from "@/components/common/toast-store";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { DICT_CODES } from "@/constants/dicts";
import { useDictOptions } from "@/hooks/use-dict-options";
import { StatusTag } from "@/components/common/status-tag";
import { formatDateTime } from "@/lib/datetime";
import { getErrorMessage } from "@/lib/api-error";
import type { DataTableColumn, GcanVehicleCanStateQuery, GcanVehicleCanStateRecord } from "@/types";

type StateFilterState = {
  vehicleName: string;
  mineId: string;
  vehicleType: string;
  boxIdHex: string;
  online: "all" | "online" | "offline";
};

const DEFAULT_FILTERS: StateFilterState = {
  vehicleName: "",
  mineId: "",
  vehicleType: "",
  boxIdHex: "",
  online: "all",
};

function buildStateQuery(filters: StateFilterState): GcanVehicleCanStateQuery {
  return {
    vehicleName: filters.vehicleName.trim() || undefined,
    mineId: filters.mineId.trim() || undefined,
    vehicleType: filters.vehicleType.trim() || undefined,
    boxIdHex: filters.boxIdHex.trim() || undefined,
    online:
      filters.online === "all" ? undefined : filters.online === "online",
  };
}

function formatMetric(value: number | string | boolean | null | undefined) {
  if (value === null || value === undefined || value === "") return "-";
  return String(value);
}

function createStateColumns(
  mineLabelMap: Map<string, string>,
  vehicleTypeLabelMap: Map<string, string>,
): DataTableColumn<GcanVehicleCanStateRecord>[] {
  return [
    {
      title: "车辆名称",
      dataIndex: "vehicleName",
      width: 150,
      render: (value) => <span className="font-medium">{String(value ?? "-")}</span>,
    },
    {
      title: "煤矿",
      dataIndex: "mineId",
      width: 140,
      render: (value) => {
        const mineId = String(value ?? "");
        const label = mineLabelMap.get(mineId) ?? mineId;
        return <span className="text-text-secondary">{label || "-"}</span>;
      },
    },
    {
      title: "车辆类型",
      dataIndex: "vehicleTypeLabel",
      width: 140,
      render: (value, record) => (
        <span className="text-text-secondary">
          {vehicleTypeLabelMap.get(record.vehicleType) ??
            String(value ?? record.vehicleType ?? "-")}
        </span>
      ),
    },
    {
      title: "盒子 ID",
      dataIndex: "boxIdHex",
      width: 110,
      render: (value, record) => (
        <span className="font-mono tabular-nums text-text-secondary">
          {String(value ?? "-")}
          <span className="ml-2 text-xs text-text-tertiary">
            ({record.boxIdDec})
          </span>
        </span>
      ),
    },
    {
      title: "在线",
      dataIndex: "online",
      width: 84,
      render: (value) => (
        <StatusTag tone={value ? "success" : "error"}>
          {value ? "在线" : "离线"}
        </StatusTag>
      ),
    },
    {
      title: "解析",
      dataIndex: "parseSupported",
      width: 110,
      render: (value, record) => (
        <StatusTag tone={value === false ? "warning" : "success"}>
          {value === false ? (record.parseMessage ?? "未支持解析") : "已支持"}
        </StatusTag>
      ),
    },
    {
      title: "最近接收",
      dataIndex: "lastReceivedAt",
      width: 180,
      render: (value) => (
        <span className="whitespace-nowrap tabular-nums">
          {formatDateTime(String(value ?? ""))}
        </span>
      ),
    },
    {
      title: "车速",
      dataIndex: "speed",
      width: 90,
      render: (value) => <span className="tabular-nums">{formatMetric(value)}</span>,
    },
    {
      title: "电压",
      dataIndex: "batteryVoltage",
      width: 90,
      render: (value) => <span className="tabular-nums">{formatMetric(value)}</span>,
    },
    {
      title: "电量",
      dataIndex: "batteryPercentage",
      width: 90,
      render: (value) => <span className="tabular-nums">{formatMetric(value)}</span>,
    },
    {
      title: "运行状态",
      dataIndex: "runState",
      width: 110,
      render: (value) => <span className="text-text-secondary">{formatMetric(value)}</span>,
    },
    {
      title: "档位",
      dataIndex: "gear",
      width: 84,
      render: (value) => <span className="text-text-secondary">{formatMetric(value)}</span>,
    },
    {
      title: "生命周期",
      dataIndex: "lifecycle",
      width: 110,
      render: (value) => <span className="text-text-secondary">{formatMetric(value)}</span>,
    },
    {
      title: "故障",
      dataIndex: "faultState",
      width: 90,
      render: (value) => <span className="text-text-secondary">{formatMetric(value)}</span>,
    },
    {
      title: "更新时间",
      dataIndex: "updateTime",
      width: 180,
      render: (value) => (
        <span className="whitespace-nowrap tabular-nums">
          {formatDateTime(String(value ?? ""))}
        </span>
      ),
    },
  ];
}

export function GcanVehicleCanStatePage() {
  const [records, setRecords] = useState<GcanVehicleCanStateRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [filters, setFilters] = useState<StateFilterState>(DEFAULT_FILTERS);
  const [appliedFilters, setAppliedFilters] = useState<StateFilterState>(DEFAULT_FILTERS);
  const mineDict = useDictOptions(DICT_CODES.GCAN_MINE);
  const vehicleTypeDict = useDictOptions(DICT_CODES.GCAN_VEHICLE_TYPE);

  const mineLabelMap = useMemo(
    () => new Map(mineDict.options.map((item) => [String(item.value), item.label])),
    [mineDict.options],
  );
  const vehicleTypeLabelMap = useMemo(
    () =>
      new Map(
        vehicleTypeDict.options.map((item) => [
          String(item.value).toUpperCase(),
          item.label,
        ]),
      ),
    [vehicleTypeDict.options],
  );

  const loadRecords = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const data = await getGcanVehicleCanStateCurrent(buildStateQuery(appliedFilters));
      setRecords(data);
    } catch (loadError) {
      setRecords([]);
      setError(getErrorMessage(loadError, "车辆 CAN 状态加载失败"));
      toast.error({
        title: "加载失败",
        description: getErrorMessage(loadError, "车辆 CAN 状态加载失败"),
      });
    } finally {
      setLoading(false);
    }
  }, [appliedFilters]);

  useEffect(() => {
    void loadRecords();
  }, [loadRecords]);

  const setFilter = <K extends keyof StateFilterState>(
    key: K,
    value: StateFilterState[K],
  ) => {
    setFilters((current) => ({ ...current, [key]: value }));
  };

  const submitFilters = () => {
    setAppliedFilters(filters);
  };

  const resetFilters = () => {
    setFilters(DEFAULT_FILTERS);
    setAppliedFilters(DEFAULT_FILTERS);
  };

  const columns = useMemo(
    () => createStateColumns(mineLabelMap, vehicleTypeLabelMap),
    [mineLabelMap, vehicleTypeLabelMap],
  );

  return (
    <div>
      <PageHeader
        title="车辆 CAN 状态"
        description="查看并筛选当前车辆的实时 CAN 状态快照。"
      />

      <SearchFilterBar
        actions={
          <>
            <Button variant="secondary" onClick={submitFilters}>
              <Search className="h-4 w-4" aria-hidden />
              查询
            </Button>
            <Button variant="secondary" onClick={resetFilters}>
              <RotateCcw className="h-4 w-4" aria-hidden />
              重置
            </Button>
          </>
        }
      >
        <Input
          placeholder="车辆名称"
          value={filters.vehicleName}
          onChange={(event) => setFilter("vehicleName", event.target.value)}
        />
        <Input
          placeholder="盒子 ID(HEX)"
          value={filters.boxIdHex}
          onChange={(event) => setFilter("boxIdHex", event.target.value)}
        />
        <Select
          value={filters.mineId}
          disabled={mineDict.loading}
          onChange={(event) => setFilter("mineId", event.target.value)}
        >
          <option value="">全部煤矿</option>
          {mineDict.options.map((item) => (
            <option key={item.value} value={item.value}>
              {item.label}
            </option>
          ))}
        </Select>
        <Select
          value={filters.vehicleType}
          disabled={vehicleTypeDict.loading}
          onChange={(event) => setFilter("vehicleType", event.target.value)}
        >
          <option value="">全部车型</option>
          {vehicleTypeDict.options.map((item) => (
            <option key={item.value} value={item.value}>
              {item.label}
            </option>
          ))}
        </Select>
        <Select
          value={filters.online}
          onChange={(event) =>
            setFilter("online", event.target.value as StateFilterState["online"])
          }
        >
          <option value="all">全部在线状态</option>
          <option value="online">在线</option>
          <option value="offline">离线</option>
        </Select>
      </SearchFilterBar>

      <div className="rounded-admin border border-border bg-surface shadow-admin">
        <TableToolbar
          title="状态列表"
          description={`共 ${records.length} 条记录`}
          actions={
            <Button variant="secondary" onClick={loadRecords}>
              <RefreshCw className="h-4 w-4" aria-hidden />
              刷新
            </Button>
          }
        />

        <DataTable
          columns={columns}
          dataSource={records}
          rowKey={(record) => record.vehicleId}
          loading={loading}
          error={error}
          minWidth={1500}
          empty={
            <EmptyState
              title="暂无 CAN 状态"
              description="当前没有可展示的车辆 CAN 状态快照。"
              actionText="刷新"
              onAction={loadRecords}
            />
          }
        />
      </div>
    </div>
  );
}
