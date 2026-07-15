import {
  Pencil,
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
  Trash2,
} from "lucide-react";
import { useCallback, useMemo, useState } from "react";
import {
  createGcanFaultProfile,
  deleteGcanFaultProfile,
  getGcanFaultProfilePage,
  updateGcanFaultProfile,
  updateGcanFaultProfileStatus,
} from "@/api/gcan-fault";
import { ConfirmDialog } from "@/components/common/confirm-dialog";
import { DataTable } from "@/components/common/data-table";
import { EmptyState } from "@/components/common/empty-state";
import { PageHeader } from "@/components/common/page-header";
import { Pagination } from "@/components/common/pagination";
import { SearchFilterBar } from "@/components/common/search-filter-bar";
import { StatusTag } from "@/components/common/status-tag";
import { TableToolbar } from "@/components/common/table-toolbar";
import { toast } from "@/components/common/toast-store";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { useListPage } from "@/hooks/use-list-page";
import { getErrorMessage } from "@/lib/api-error";
import { formatDateTime } from "@/lib/datetime";
import type { ApiStatus, DataTableColumn } from "@/types";
import type { GcanFaultProfileRecord } from "@/types/gcan-fault";
import {
  FaultProfileFormDialog,
  type ProfileFormValues,
} from "../faults/fault-form-dialogs";

type ProfileFilters = {
  profileCode: string;
  profileName: string;
  manufacturer: string;
  status: "all" | ApiStatus;
};

const DEFAULT_FILTERS: ProfileFilters = {
  profileCode: "",
  profileName: "",
  manufacturer: "",
  status: "all",
};

function statusTone(status: ApiStatus) {
  return status === 1 ? "success" : "neutral";
}

export function GcanFaultProfilesPage() {
  const [formOpen, setFormOpen] = useState(false);
  const [formMode, setFormMode] = useState<"create" | "edit">("create");
  const [editingRecord, setEditingRecord] = useState<GcanFaultProfileRecord | null>(null);
  const [formSubmitting, setFormSubmitting] = useState(false);
  const [deletingRecord, setDeletingRecord] = useState<GcanFaultProfileRecord | null>(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [statusLoadingId, setStatusLoadingId] = useState<number | null>(null);

  const list = useListPage<ProfileFilters, GcanFaultProfileRecord>({
    fetch: getGcanFaultProfilePage,
    defaultFilters: DEFAULT_FILTERS,
    toQuery: (filters, page, pageSize) => ({
      page,
      pageSize,
      profileCode: filters.profileCode.trim() || undefined,
      profileName: filters.profileName.trim() || undefined,
      manufacturer: filters.manufacturer.trim() || undefined,
      status: filters.status === "all" ? undefined : filters.status,
    }),
    onError: (error) => toast.error({ title: "加载失败", description: getErrorMessage(error, "故障码表加载失败") }),
  });
  const reload = list.reload;

  const openCreateForm = () => {
    setFormMode("create");
    setEditingRecord(null);
    setFormOpen(true);
  };

  const openEditForm = (record: GcanFaultProfileRecord) => {
    setFormMode("edit");
    setEditingRecord(record);
    setFormOpen(true);
  };

  const submitForm = async (values: ProfileFormValues) => {
    setFormSubmitting(true);
    const payload = Object.fromEntries(
      Object.entries(values).map(([key, value]) => [key, typeof value === "string" ? value.trim() || undefined : value]),
    ) as ProfileFormValues;
    try {
      if (formMode === "edit" && editingRecord) {
        await updateGcanFaultProfile(editingRecord.id, payload);
        toast.success("故障码表已更新");
      } else {
        await createGcanFaultProfile(payload);
        toast.success("故障码表已创建");
      }
      setFormOpen(false);
      await list.reload();
    } catch (error) {
      toast.error({ title: "保存失败", description: getErrorMessage(error, "请检查表单后重试") });
    } finally {
      setFormSubmitting(false);
    }
  };

  const confirmDelete = async () => {
    if (!deletingRecord) return;
    setDeleteLoading(true);
    try {
      await deleteGcanFaultProfile(deletingRecord.id);
      toast.success("故障码表已删除");
      setDeletingRecord(null);
      await list.reload();
    } catch (error) {
      toast.error({ title: "删除失败", description: getErrorMessage(error, "该故障码表可能已被车辆或故障定义关联") });
    } finally {
      setDeleteLoading(false);
    }
  };

  const toggleStatus = useCallback(async (record: GcanFaultProfileRecord) => {
    setStatusLoadingId(record.id);
    try {
      await updateGcanFaultProfileStatus(record.id, record.status === 1 ? 0 : 1);
      toast.success(record.status === 1 ? "故障码表已停用" : "故障码表已启用");
      await reload();
    } catch (error) {
      toast.error({ title: "状态更新失败", description: getErrorMessage(error, "请稍后重试") });
    } finally {
      setStatusLoadingId(null);
    }
  }, [reload]);

  const columns = useMemo<DataTableColumn<GcanFaultProfileRecord>[]>(() => [
    { title: "故障表名称", dataIndex: "profileName", width: 190, render: (value) => <span className="font-medium text-text-primary">{String(value || "-")}</span> },
    { title: "故障表编码", dataIndex: "profileCode", width: 170, render: (value) => <span className="font-mono text-text-secondary">{String(value || "-")}</span> },
    { title: "厂家 / 车型", key: "vehicle", width: 190, render: (_, record) => <span>{[record.manufacturer, record.vehicleType].filter(Boolean).join(" · ") || "-"}</span> },
    { title: "协议版本", dataIndex: "protocolVersion", width: 110, render: (value) => String(value || "-") },
    { title: "状态", dataIndex: "status", width: 90, render: (value) => <StatusTag tone={statusTone(Number(value) as ApiStatus)}>{Number(value) === 1 ? "启用" : "停用"}</StatusTag> },
    { title: "更新时间", dataIndex: "updateTime", width: 170, render: (value) => <span className="whitespace-nowrap tabular-nums">{formatDateTime(String(value ?? ""))}</span> },
    { title: "操作", key: "actions", align: "center", width: 250, render: (_, record) => <div className="inline-flex items-center justify-center gap-1"><Button size="sm" variant="ghost" onClick={() => openEditForm(record)}><Pencil className="h-4 w-4" aria-hidden />编辑</Button><Button size="sm" variant="ghost" disabled={statusLoadingId === record.id} onClick={() => void toggleStatus(record)}>{record.status === 1 ? "停用" : "启用"}</Button><Button size="sm" variant="ghost" className="text-error hover:text-error" onClick={() => setDeletingRecord(record)}><Trash2 className="h-4 w-4" aria-hidden />删除</Button></div> },
  ], [statusLoadingId, toggleStatus]);

  return (
    <div>
      <PageHeader title="故障码表维护" description="按车型独立维护厂家故障码表，并将故障定义挂载到对应表中。" actions={<Button variant="primary" onClick={openCreateForm}><Plus className="h-4 w-4" aria-hidden />新增故障码表</Button>} />
      <div className="mb-5 grid gap-3 md:grid-cols-3">
        <div className="rounded-admin border border-border bg-surface px-4 py-3 shadow-admin"><p className="text-xs text-text-tertiary">当前结果</p><p className="mt-1 text-2xl font-semibold text-text-primary">{list.total}</p></div>
        <div className="rounded-admin border border-border bg-surface px-4 py-3 shadow-admin"><p className="text-xs text-text-tertiary">启用表</p><p className="mt-1 text-2xl font-semibold text-success">{list.data.filter((item) => item.status === 1).length}</p></div>
        <div className="rounded-admin border border-amber-200 bg-amber-50/50 px-4 py-3 shadow-admin"><p className="text-xs text-amber-700">维护提示</p><p className="mt-1 text-sm text-amber-900">编码创建后不可修改，避免车辆关联失效。</p></div>
      </div>
      <SearchFilterBar actions={<><Button variant="secondary" onClick={list.submitFilters}><Search className="h-4 w-4" aria-hidden />查询</Button><Button variant="secondary" onClick={list.resetFilters}><RotateCcw className="h-4 w-4" aria-hidden />重置</Button></>}>
        <Input placeholder="故障表编码" value={list.filters.profileCode} onChange={(event) => list.setFilter("profileCode", event.target.value)} />
        <Input placeholder="故障表名称" value={list.filters.profileName} onChange={(event) => list.setFilter("profileName", event.target.value)} />
        <Input placeholder="车辆厂家" value={list.filters.manufacturer} onChange={(event) => list.setFilter("manufacturer", event.target.value)} />
        <Select value={String(list.filters.status)} onChange={(event) => list.setFilter("status", event.target.value === "all" ? "all" : Number(event.target.value) as ApiStatus)}><option value="all">全部状态</option><option value="1">启用</option><option value="0">停用</option></Select>
      </SearchFilterBar>
      <div className="rounded-admin border border-border bg-surface shadow-admin"><TableToolbar title="车型故障码表" description="相同内容也可按车型分别录入，表之间互不影响。" actions={<Button variant="secondary" onClick={list.reload}><RefreshCw className="h-4 w-4" aria-hidden />刷新</Button>} /><DataTable columns={columns} dataSource={list.data} rowKey="id" loading={list.loading} error={list.error} minWidth={1100} empty={<EmptyState title="暂无故障码表" description="先建立车型对应的厂家故障码表。" actionText="新增故障码表" onAction={openCreateForm} />} /><Pagination page={list.page} pageSize={list.pageSize} total={list.total} disabled={list.loading} onPageChange={list.setPage} onPageSizeChange={list.setPageSize} /></div>
      <FaultProfileFormDialog open={formOpen} mode={formMode} record={editingRecord} submitting={formSubmitting} onSubmit={submitForm} onCancel={() => setFormOpen(false)} />
      <ConfirmDialog open={Boolean(deletingRecord)} title="删除故障码表" description={`确认删除「${deletingRecord?.profileName ?? ""}」吗？如果它已关联车辆或故障定义，后端会拒绝删除。`} confirmText="删除" danger loading={deleteLoading} onConfirm={() => void confirmDelete()} onCancel={() => setDeletingRecord(null)} />
    </div>
  );
}
