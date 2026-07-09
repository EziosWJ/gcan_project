import { RefreshCw } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { getGcanVehicleCanStateCurrent } from "@/api/gcan";
import { DataTable } from "@/components/common/data-table";
import { EmptyState } from "@/components/common/empty-state";
import { PageHeader } from "@/components/common/page-header";
import { TableToolbar } from "@/components/common/table-toolbar";
import { toast } from "@/components/common/toast-store";
import { Button } from "@/components/ui/button";
import { StatusTag } from "@/components/common/status-tag";
import { formatDateTime } from "@/lib/datetime";
import { getErrorMessage } from "@/lib/api-error";
import type { DataTableColumn, GcanVehicleCanStateRecord } from "@/types";

function formatMetric(value: number | string | boolean | null | undefined) {
  if (value === null || value === undefined || value === "") return "-";
  return String(value);
}

function createStateColumns(): DataTableColumn<GcanVehicleCanStateRecord>[] {
  return [
    {
      title: "车辆名称",
      dataIndex: "vehicleName",
      width: 150,
      render: (value) => <span className="font-medium">{String(value ?? "-")}</span>,
    },
    {
      title: "车辆类型",
      dataIndex: "vehicleTypeLabel",
      width: 140,
      render: (value, record) => (
        <span className="text-text-secondary">
          {String(value ?? record.vehicleType ?? "-")}
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

  const loadRecords = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const data = await getGcanVehicleCanStateCurrent();
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
  }, []);

  useEffect(() => {
    void loadRecords();
  }, [loadRecords]);

  const columns = useMemo(() => createStateColumns(), []);

  return (
    <div>
      <PageHeader
        title="车辆 CAN 状态"
        description="查看当前车辆的实时 CAN 状态快照，仅供只读查看。"
      />

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
