import { Pencil, Power, Trash2 } from "lucide-react";
import { ApiStatusTag } from "@/components/common/api-status-tag";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { formatDateTime } from "@/lib/datetime";
import type { DataTableColumn, GcanVehicleRecord } from "@/types";

type VehicleColumnActions = {
  selectedIds: Set<number>;
  allChecked: boolean;
  selectableCount: number;
  mineLabelMap: Map<string, string>;
  vehicleTypeLabelMap: Map<string, string>;
  onToggleSelect: (id: number, checked: boolean) => void;
  onToggleSelectAll: (checked: boolean) => void;
  onEdit: (record: GcanVehicleRecord) => void;
  onToggleStatus: (record: GcanVehicleRecord) => void;
  onDelete: (record: GcanVehicleRecord) => void;
};

export function createVehicleColumns(
  actions: VehicleColumnActions,
): DataTableColumn<GcanVehicleRecord>[] {
  return [
    {
      title: (
        <Checkbox
          aria-label="选择当前页车辆"
          checked={actions.allChecked}
          disabled={actions.selectableCount === 0}
          onChange={(event) => actions.onToggleSelectAll(event.target.checked)}
        />
      ),
      key: "selection",
      align: "center",
      width: 54,
      render: (_, record) => (
        <Checkbox
          aria-label={`选择车辆 ${record.vehicleName}`}
          checked={actions.selectedIds.has(record.id)}
          onChange={(event) =>
            actions.onToggleSelect(record.id, event.target.checked)
          }
        />
      ),
    },
    {
      title: "车辆名称",
      dataIndex: "vehicleName",
      width: 180,
      render: (value) => <span className="font-medium">{String(value ?? "-")}</span>,
    },
    {
      title: "煤矿",
      dataIndex: "mineId",
      width: 150,
      render: (value, record) => {
        const mineId = String(value ?? "");
        const label = record.mineName ?? actions.mineLabelMap.get(mineId) ?? mineId;
        return (
          <span className="text-text-secondary">
            {label || "-"}
          </span>
        );
      },
    },
    {
      title: "车辆类型",
      dataIndex: "vehicleTypeLabel",
      width: 150,
      render: (value, record) => (
        <span className="text-text-secondary">
          {actions.vehicleTypeLabelMap.get(record.vehicleType) ??
            String(value ?? record.vehicleType ?? "-")}
        </span>
      ),
    },
    {
      title: "接入方式",
      dataIndex: "accessMode",
      width: 120,
      render: (value, record) => (
        <span className="text-text-secondary">
          {record.accessMode === "MINE_API" ? "煤矿接口" : "GCAN"}
        </span>
      ),
    },
    {
      title: "接入标识",
      dataIndex: "boxIdHex",
      width: 150,
      render: (value, record) => (
        <span className="font-mono tabular-nums text-text-secondary">
          {record.accessMode === "MINE_API" ? (record.externalVehicleCode ?? "-") : (
            <>
              {String(value ?? "-")}
              <span className="ml-2 text-xs text-text-tertiary">({record.boxIdDec ?? "-"})</span>
            </>
          )}
        </span>
      ),
    },
    {
      title: "状态",
      dataIndex: "status",
      width: 96,
      render: (_, record) => <ApiStatusTag status={record.status} />,
    },
    {
      title: "备注",
      dataIndex: "remark",
      width: 180,
      render: (value) => (
        <span className="block max-w-[180px] truncate text-text-secondary">
          {String(value || "-")}
        </span>
      ),
    },
    {
      title: "创建时间",
      dataIndex: "createTime",
      width: 170,
      render: (value) => (
        <span className="whitespace-nowrap tabular-nums">
          {formatDateTime(String(value ?? ""))}
        </span>
      ),
    },
    {
      title: "更新时间",
      dataIndex: "updateTime",
      width: 170,
      render: (value) => (
        <span className="whitespace-nowrap tabular-nums">
          {formatDateTime(String(value ?? ""))}
        </span>
      ),
    },
    {
      title: "操作",
      key: "actions",
      align: "center",
      width: 240,
      render: (_, record) => {
        const nextStatus = record.status === 1 ? 0 : 1;

        return (
          <div className="inline-flex flex-wrap items-center justify-center gap-1">
            <Button size="sm" variant="ghost" onClick={() => actions.onEdit(record)}>
              <Pencil className="h-4 w-4" aria-hidden />
              编辑
            </Button>
            <Button
              size="sm"
              variant="ghost"
              onClick={() => actions.onToggleStatus(record)}
            >
              <Power className="h-4 w-4" aria-hidden />
              {nextStatus === 1 ? "启用" : "禁用"}
            </Button>
            <Button
              size="sm"
              variant="ghost"
              className="text-error hover:text-error"
              onClick={() => actions.onDelete(record)}
            >
              <Trash2 className="h-4 w-4" aria-hidden />
              删除
            </Button>
          </div>
        );
      },
    },
  ];
}
