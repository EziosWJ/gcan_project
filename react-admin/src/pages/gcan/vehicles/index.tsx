import {
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
  Trash2,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  batchDeleteGcanVehicles,
  createGcanVehicle,
  deleteGcanVehicle,
  getGcanVehiclePage,
  getGcanVehicleTypes,
  updateGcanVehicle,
  updateGcanVehicleStatus,
} from "@/api/gcan";
import { ConfirmDialog } from "@/components/common/confirm-dialog";
import { DataTable } from "@/components/common/data-table";
import { EmptyState } from "@/components/common/empty-state";
import { PageHeader } from "@/components/common/page-header";
import { Pagination } from "@/components/common/pagination";
import { SearchFilterBar } from "@/components/common/search-filter-bar";
import { TableToolbar } from "@/components/common/table-toolbar";
import { toast } from "@/components/common/toast-store";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { useListPage } from "@/hooks/use-list-page";
import { getErrorMessage } from "@/lib/api-error";
import type { ApiStatus, GcanVehicleRecord, GcanVehicleTypeRecord } from "@/types";
import { createVehicleColumns } from "./columns";
import { VehicleFormDialog } from "./vehicle-form-dialog";
import {
  buildVehiclePayload,
  buildVehicleQuery,
  DEFAULT_FILTERS,
  type VehicleFilterState,
  type VehicleFormMode,
  type VehicleFormValues,
} from "./schema";

type ConfirmAction =
  | { type: "delete"; vehicle: GcanVehicleRecord }
  | { type: "status"; vehicle: GcanVehicleRecord; status: ApiStatus }
  | { type: "batchDelete"; vehicles: GcanVehicleRecord[] };

export function GcanVehiclesPage() {
  const {
    data: vehicles,
    total,
    loading,
    error,
    page,
    pageSize,
    setPage,
    setPageSize,
    filters,
    setFilter,
    submitFilters,
    resetFilters,
    reload: loadVehicles,
  } = useListPage<VehicleFilterState, GcanVehicleRecord>({
    fetch: getGcanVehiclePage,
    defaultFilters: DEFAULT_FILTERS,
    toQuery: (f, p, ps) => buildVehicleQuery(f, p, ps),
    onError: (err) =>
      toast.error({
        title: "加载失败",
        description: getErrorMessage(err, "车辆档案加载失败"),
      }),
  });

  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [vehicleTypes, setVehicleTypes] = useState<GcanVehicleTypeRecord[]>([]);
  const [vehicleTypesLoading, setVehicleTypesLoading] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [formMode, setFormMode] = useState<VehicleFormMode>("create");
  const [editingVehicle, setEditingVehicle] = useState<GcanVehicleRecord | null>(null);
  const [formSubmitting, setFormSubmitting] = useState(false);
  const [confirmAction, setConfirmAction] = useState<ConfirmAction | null>(null);
  const [confirmLoading, setConfirmLoading] = useState(false);

  useEffect(() => {
    setSelectedIds((current) => {
      const visibleIds = new Set(vehicles.map((item) => item.id));
      return new Set([...current].filter((id) => visibleIds.has(id)));
    });
  }, [vehicles]);

  useEffect(() => {
    let active = true;
    setVehicleTypesLoading(true);

    void getGcanVehicleTypes()
      .then((data) => {
        if (!active) return;
        setVehicleTypes(data);
      })
      .catch((err) => {
        if (!active) return;
        toast.error({
          title: "车辆类型加载失败",
          description: getErrorMessage(err, "无法获取车辆类型列表"),
        });
      })
      .finally(() => {
        if (active) setVehicleTypesLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const toggleSelect = useCallback((id: number, checked: boolean) => {
    setSelectedIds((current) => {
      const next = new Set(current);
      if (checked) next.add(id);
      else next.delete(id);
      return next;
    });
  }, []);

  const toggleSelectAll = useCallback(
    (checked: boolean) => {
      setSelectedIds((current) => {
        const next = new Set(current);
        vehicles.forEach((item) => {
          if (checked) next.add(item.id);
          else next.delete(item.id);
        });
        return next;
      });
    },
    [vehicles],
  );

  const selectedVehicles = useMemo(
    () => vehicles.filter((item) => selectedIds.has(item.id)),
    [selectedIds, vehicles],
  );

  const allChecked = useMemo(
    () => vehicles.length > 0 && vehicles.every((item) => selectedIds.has(item.id)),
    [selectedIds, vehicles],
  );

  const openCreateForm = () => {
    setFormMode("create");
    setEditingVehicle(null);
    setFormOpen(true);
  };

  const openEditForm = (vehicle: GcanVehicleRecord) => {
    setFormMode("edit");
    setEditingVehicle(vehicle);
    setFormOpen(true);
  };

  const submitVehicleForm = async (values: VehicleFormValues) => {
    setFormSubmitting(true);
    try {
      if (formMode === "edit" && editingVehicle) {
        await updateGcanVehicle(editingVehicle.id, buildVehiclePayload(values));
        toast.success("车辆档案已更新");
      } else {
        await createGcanVehicle(buildVehiclePayload(values));
        toast.success("车辆档案已创建");
      }

      setFormOpen(false);
      await loadVehicles();
    } catch (submitError) {
      toast.error({
        title: formMode === "edit" ? "更新失败" : "创建失败",
        description: getErrorMessage(submitError, "请检查表单后重试"),
      });
    } finally {
      setFormSubmitting(false);
    }
  };

  const runConfirmAction = async () => {
    if (!confirmAction) return;

    setConfirmLoading(true);
    try {
      if (confirmAction.type === "delete") {
        await deleteGcanVehicle(confirmAction.vehicle.id);
        toast.success("车辆已删除");
      }

      if (confirmAction.type === "status") {
        await updateGcanVehicleStatus(confirmAction.vehicle.id, {
          status: confirmAction.status,
        });
        toast.success(confirmAction.status === 1 ? "车辆已启用" : "车辆已禁用");
      }

      if (confirmAction.type === "batchDelete") {
        await batchDeleteGcanVehicles({
          ids: confirmAction.vehicles.map((item) => item.id),
        });
        toast.success("车辆已批量删除");
      }

      setConfirmAction(null);
      setSelectedIds(new Set());
      await loadVehicles();
    } catch (actionError) {
      toast.error({
        title: "操作失败",
        description: getErrorMessage(actionError, "请稍后重试"),
      });
    } finally {
      setConfirmLoading(false);
    }
  };

  const confirmMeta = useMemo(() => {
    if (!confirmAction) return null;

    if (confirmAction.type === "delete") {
      return {
        title: "删除车辆档案",
        description: `确认删除车辆「${confirmAction.vehicle.vehicleName}」吗？此操作不可恢复。`,
        confirmText: "删除",
        danger: true,
      };
    }

    if (confirmAction.type === "status") {
      return {
        title: confirmAction.status === 1 ? "启用车辆" : "禁用车辆",
        description: `确认将车辆「${confirmAction.vehicle.vehicleName}」${
          confirmAction.status === 1 ? "启用" : "禁用"
        }吗？`,
        confirmText: confirmAction.status === 1 ? "启用" : "禁用",
        danger: confirmAction.status === 0,
      };
    }

    return {
      title: "批量删除车辆",
      description: `确认删除选中的 ${confirmAction.vehicles.length} 条车辆档案吗？此操作不可恢复。`,
      confirmText: "删除",
      danger: true,
    };
  }, [confirmAction]);

  const columns = useMemo(
    () =>
      createVehicleColumns({
        selectedIds,
        allChecked,
        selectableCount: vehicles.length,
        onToggleSelect: toggleSelect,
        onToggleSelectAll: toggleSelectAll,
        onEdit: openEditForm,
        onToggleStatus: (vehicle) =>
          setConfirmAction({
            type: "status",
            vehicle,
            status: vehicle.status === 1 ? 0 : 1,
          }),
        onDelete: (vehicle) => setConfirmAction({ type: "delete", vehicle }),
      }),
    [
      allChecked,
      openEditForm,
      selectedIds,
      toggleSelect,
      toggleSelectAll,
      vehicles.length,
    ],
  );

  return (
    <div>
      <PageHeader
        title="车辆档案管理"
        description="维护 GCAN 车辆基础档案，支持筛选、新增、编辑、状态切换和批量删除。"
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
            <Button variant="primary" onClick={openCreateForm}>
              <Plus className="h-4 w-4" aria-hidden />
              新增
            </Button>
          </>
        }
      >
        <Input
          placeholder="车辆名称"
          value={filters.vehicleName}
          onChange={(event) => setFilter("vehicleName", event.target.value)}
        />
        <Select
          value={filters.vehicleType}
          onChange={(event) => setFilter("vehicleType", event.target.value)}
          disabled={vehicleTypesLoading}
        >
          <option value="">全部车辆类型</option>
          {vehicleTypes.map((item) => (
            <option key={item.code} value={item.code}>
              {item.label}
            </option>
          ))}
        </Select>
        <Input
          placeholder="盒子 ID(HEX)"
          value={filters.boxIdHex}
          onChange={(event) => setFilter("boxIdHex", event.target.value)}
        />
        <Select
          value={filters.status}
          onChange={(event) =>
            setFilter("status", event.target.value === "all" ? "all" : Number(event.target.value) as ApiStatus)
          }
        >
          <option value="all">全部状态</option>
          <option value={1}>启用</option>
          <option value={0}>禁用</option>
        </Select>
      </SearchFilterBar>

      <div className="rounded-admin border border-border bg-surface shadow-admin">
        <TableToolbar
          title="车辆列表"
          description={selectedVehicles.length > 0 ? `已选择 ${selectedVehicles.length} 条记录` : "当前页车辆档案"}
          actions={
            <>
              <Button
                variant="secondary"
                disabled={selectedVehicles.length === 0}
                onClick={() =>
                  setConfirmAction({ type: "batchDelete", vehicles: selectedVehicles })
                }
              >
                <Trash2 className="h-4 w-4" aria-hidden />
                批量删除
              </Button>
              <Button variant="secondary" onClick={loadVehicles}>
                <RefreshCw className="h-4 w-4" aria-hidden />
                刷新
              </Button>
            </>
          }
        />

        <DataTable
          columns={columns}
          dataSource={vehicles}
          rowKey="id"
          loading={loading}
          error={error}
          minWidth={1320}
          empty={
            <EmptyState
              title="暂无车辆档案"
              description="请先新增车辆档案，或调整筛选条件后重试。"
              actionText="新增车辆"
              onAction={openCreateForm}
            />
          }
        />

        <Pagination
          page={page}
          pageSize={pageSize}
          total={total}
          disabled={loading}
          onPageChange={setPage}
          onPageSizeChange={setPageSize}
        />
      </div>

      <VehicleFormDialog
        open={formOpen}
        mode={formMode}
        record={editingVehicle}
        vehicleTypes={vehicleTypes}
        submitting={formSubmitting}
        onSubmit={submitVehicleForm}
        onCancel={() => setFormOpen(false)}
      />

      <ConfirmDialog
        open={Boolean(confirmAction && confirmMeta)}
        title={confirmMeta?.title ?? ""}
        description={confirmMeta?.description}
        confirmText={confirmMeta?.confirmText}
        danger={confirmMeta?.danger}
        loading={confirmLoading}
        onConfirm={runConfirmAction}
        onCancel={() => setConfirmAction(null)}
      />
    </div>
  );
}
