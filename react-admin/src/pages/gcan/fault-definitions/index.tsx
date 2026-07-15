import {
  Pencil,
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
  Trash2,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  createGcanFaultDefinition,
  deleteGcanFaultDefinition,
  getGcanFaultDefinitionPage,
  getGcanFaultProfilePage,
  updateGcanFaultDefinition,
  updateGcanFaultDefinitionStatus,
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
import type { ApiStatus, DataTableColumn } from "@/types";
import type { GcanFaultDefinitionRecord, GcanFaultProfileRecord } from "@/types/gcan-fault";
import {
  FaultDefinitionFormDialog,
  type DefinitionFormValues,
} from "../faults/fault-form-dialogs";

type DefinitionFilters = {
  profileCode: string;
  faultCode: string;
  faultName: string;
  status: "all" | ApiStatus;
};

const DEFAULT_FILTERS: DefinitionFilters = {
  profileCode: "",
  faultCode: "",
  faultName: "",
  status: "all",
};

export function GcanFaultDefinitionsPage() {
  const [profiles, setProfiles] = useState<GcanFaultProfileRecord[]>([]);
  const [profilesLoading, setProfilesLoading] = useState(true);
  const [formOpen, setFormOpen] = useState(false);
  const [formMode, setFormMode] = useState<"create" | "edit">("create");
  const [editingRecord, setEditingRecord] = useState<GcanFaultDefinitionRecord | null>(null);
  const [formSubmitting, setFormSubmitting] = useState(false);
  const [deletingRecord, setDeletingRecord] = useState<GcanFaultDefinitionRecord | null>(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [statusLoadingId, setStatusLoadingId] = useState<number | null>(null);
  const enabledProfileCount = profiles.filter((profile) => profile.status === 1).length;

  const loadProfiles = async () => {
    setProfilesLoading(true);
    try {
      const result = await getGcanFaultProfilePage({ page: 1, pageSize: 100 });
      setProfiles(result.records);
    } catch (error) {
      toast.error({ title: "故障码表加载失败", description: getErrorMessage(error, "新增故障定义前请先建立故障码表") });
    } finally {
      setProfilesLoading(false);
    }
  };

  useEffect(() => {
    void loadProfiles();
  }, []);

  const list = useListPage<DefinitionFilters, GcanFaultDefinitionRecord>({
    fetch: getGcanFaultDefinitionPage,
    defaultFilters: DEFAULT_FILTERS,
    toQuery: (filters, page, pageSize) => ({
      page,
      pageSize,
      profileCode: filters.profileCode.trim() || undefined,
      faultCode: filters.faultCode.trim() || undefined,
      faultName: filters.faultName.trim() || undefined,
      status: filters.status === "all" ? undefined : filters.status,
    }),
    onError: (error) => toast.error({ title: "加载失败", description: getErrorMessage(error, "故障定义加载失败") }),
  });
  const reload = list.reload;

  const openCreateForm = () => {
    setFormMode("create");
    setEditingRecord(null);
    setFormOpen(true);
  };

  const openEditForm = (record: GcanFaultDefinitionRecord) => {
    setFormMode("edit");
    setEditingRecord(record);
    setFormOpen(true);
  };

  const submitForm = async (values: DefinitionFormValues) => {
    setFormSubmitting(true);
    const payload = Object.fromEntries(
      Object.entries(values).map(([key, value]) => [key, typeof value === "string" ? value.trim() || undefined : value]),
    ) as DefinitionFormValues;
    try {
      if (formMode === "edit" && editingRecord) {
        await updateGcanFaultDefinition(editingRecord.id, payload);
        toast.success("故障定义已更新");
      } else {
        await createGcanFaultDefinition(payload);
        toast.success("故障定义已创建");
      }
      setFormOpen(false);
      await list.reload();
    } catch (error) {
      toast.error({ title: "保存失败", description: getErrorMessage(error, "请检查故障码是否重复") });
    } finally {
      setFormSubmitting(false);
    }
  };

  const confirmDelete = async () => {
    if (!deletingRecord) return;
    setDeleteLoading(true);
    try {
      await deleteGcanFaultDefinition(deletingRecord.id);
      toast.success("故障定义已删除");
      setDeletingRecord(null);
      await list.reload();
    } catch (error) {
      toast.error({ title: "删除失败", description: getErrorMessage(error, "请稍后重试") });
    } finally {
      setDeleteLoading(false);
    }
  };

  const toggleStatus = useCallback(async (record: GcanFaultDefinitionRecord) => {
    setStatusLoadingId(record.id);
    try {
      await updateGcanFaultDefinitionStatus(record.id, record.status === 1 ? 0 : 1);
      toast.success(record.status === 1 ? "故障定义已停用" : "故障定义已启用");
      await reload();
    } catch (error) {
      toast.error({ title: "状态更新失败", description: getErrorMessage(error, "请稍后重试") });
    } finally {
      setStatusLoadingId(null);
    }
  }, [reload]);

  const columns = useMemo<DataTableColumn<GcanFaultDefinitionRecord>[]>(() => [
    { title: "故障码", dataIndex: "faultCode", width: 130, render: (value) => <span className="font-mono font-medium text-text-primary">{String(value || "-")}</span> },
    { title: "故障名称", dataIndex: "faultName", width: 190, render: (value) => <span className="font-medium">{String(value || "未填写")}</span> },
    { title: "故障码表", dataIndex: "profileCode", width: 170, render: (value) => <span className="font-mono text-text-secondary">{String(value || "-")}</span> },
    { title: "等级", key: "level", width: 120, render: (_, record) => <span>{record.rawLevelName || record.rawLevelCode || "-"}</span> },
    { title: "处理建议", dataIndex: "handlingSuggestion", width: 260, render: (value) => <span className="block max-w-[260px] truncate text-text-secondary" title={String(value || "")}>{String(value || "-" )}</span> },
    { title: "状态", dataIndex: "status", width: 90, render: (value) => <StatusTag tone={Number(value) === 1 ? "success" : "neutral"}>{Number(value) === 1 ? "启用" : "停用"}</StatusTag> },
    { title: "操作", key: "actions", align: "center", width: 250, render: (_, record) => <div className="inline-flex items-center justify-center gap-1"><Button size="sm" variant="ghost" onClick={() => openEditForm(record)}><Pencil className="h-4 w-4" aria-hidden />编辑</Button><Button size="sm" variant="ghost" disabled={statusLoadingId === record.id} onClick={() => void toggleStatus(record)}>{record.status === 1 ? "停用" : "启用"}</Button><Button size="sm" variant="ghost" className="text-error hover:text-error" onClick={() => setDeletingRecord(record)}><Trash2 className="h-4 w-4" aria-hidden />删除</Button></div> },
  ], [statusLoadingId, toggleStatus]);

  return (
    <div>
      <PageHeader title="故障定义维护" description="维护厂家故障码的中文翻译、故障现象、原因分析和现场处理建议。" actions={<Button variant="primary" disabled={profilesLoading || enabledProfileCount === 0} onClick={openCreateForm}><Plus className="h-4 w-4" aria-hidden />新增故障定义</Button>} />
      <SearchFilterBar actions={<><Button variant="secondary" onClick={list.submitFilters}><Search className="h-4 w-4" aria-hidden />查询</Button><Button variant="secondary" onClick={list.resetFilters}><RotateCcw className="h-4 w-4" aria-hidden />重置</Button></>}>
        <Select value={list.filters.profileCode} onChange={(event) => list.setFilter("profileCode", event.target.value)}><option value="">全部故障码表</option>{profiles.map((profile) => <option key={profile.profileCode} value={profile.profileCode}>{profile.profileName}</option>)}</Select>
        <Input placeholder="故障码" value={list.filters.faultCode} onChange={(event) => list.setFilter("faultCode", event.target.value)} />
        <Input placeholder="故障名称" value={list.filters.faultName} onChange={(event) => list.setFilter("faultName", event.target.value)} />
        <Select value={String(list.filters.status)} onChange={(event) => list.setFilter("status", event.target.value === "all" ? "all" : Number(event.target.value) as ApiStatus)}><option value="all">全部状态</option><option value="1">启用</option><option value="0">停用</option></Select>
      </SearchFilterBar>
      <div className="rounded-admin border border-border bg-surface shadow-admin"><TableToolbar title="故障定义清单" description={enabledProfileCount ? `当前启用故障码表 ${enabledProfileCount} 份，故障定义按表隔离。` : "请先在故障码表维护中建立并启用故障码表。"} actions={<Button variant="secondary" onClick={() => { void Promise.all([list.reload(), loadProfiles()]); }}><RefreshCw className="h-4 w-4" aria-hidden />刷新</Button>} /><DataTable columns={columns} dataSource={list.data} rowKey="id" loading={list.loading} error={list.error} minWidth={1250} empty={<EmptyState title="暂无故障定义" description={enabledProfileCount ? "为车型故障码表补充故障翻译和处理建议。" : "请先建立并启用故障码表。"} actionText={enabledProfileCount ? "新增故障定义" : undefined} onAction={enabledProfileCount ? openCreateForm : undefined} />} /><Pagination page={list.page} pageSize={list.pageSize} total={list.total} disabled={list.loading} onPageChange={list.setPage} onPageSizeChange={list.setPageSize} /></div>
      <FaultDefinitionFormDialog open={formOpen} mode={formMode} record={editingRecord} profileOptions={profiles} submitting={formSubmitting} onSubmit={submitForm} onCancel={() => setFormOpen(false)} />
      <ConfirmDialog open={Boolean(deletingRecord)} title="删除故障定义" description={`确认删除「${deletingRecord?.faultCode ?? ""} ${deletingRecord?.faultName ?? ""}」吗？`} confirmText="删除" danger loading={deleteLoading} onConfirm={() => void confirmDelete()} onCancel={() => setDeletingRecord(null)} />
    </div>
  );
}
