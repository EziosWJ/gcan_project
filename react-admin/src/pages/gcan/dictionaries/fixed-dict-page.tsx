import { zodResolver } from "@hookform/resolvers/zod";
import {
  Pencil,
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
  Trash2,
  X,
} from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { createPortal } from "react-dom";
import { z } from "zod";
import {
  clearDictItemCache,
  createDictData,
  createDictType,
  deleteDictData,
  getDictDataPage,
  getDictTypePage,
  updateDictData,
} from "@/api/system";
import { ConfirmDialog } from "@/components/common/confirm-dialog";
import { DataTable } from "@/components/common/data-table";
import { EmptyState } from "@/components/common/empty-state";
import { Field } from "@/components/common/field";
import { PageHeader } from "@/components/common/page-header";
import { Pagination } from "@/components/common/pagination";
import { SearchFilterBar } from "@/components/common/search-filter-bar";
import { TableToolbar } from "@/components/common/table-toolbar";
import { toast } from "@/components/common/toast-store";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useListPage } from "@/hooks/use-list-page";
import { getErrorMessage } from "@/lib/api-error";
import { formatDateTime } from "@/lib/datetime";
import type {
  DataTableColumn,
  DictDataListQuery,
  SystemDictDataRecord,
} from "@/types";

type FixedDictPageProps = {
  title: string;
  description: string;
  dictCode: string;
  sortOrder?: number;
  labelName: string;
  valueName: string;
  valueHelp?: string;
};

type FixedDictFilterState = {
  dictLabel: string;
  dictValue: string;
};

type FormMode = "create" | "edit";

const DEFAULT_FILTERS: FixedDictFilterState = {
  dictLabel: "",
  dictValue: "",
};

const fixedDictFormSchema = z.object({
  dictLabel: z
    .string()
    .trim()
    .min(1, "名称不能为空")
    .max(64, "名称不能超过 64 个字符"),
  dictValue: z
    .string()
    .trim()
    .min(1, "编码不能为空")
    .max(128, "编码不能超过 128 个字符"),
  sortOrder: z.coerce.number().int("排序必须是整数").min(0, "排序不能小于 0"),
  remark: z.string().trim().max(200, "备注不能超过 200 个字符").optional(),
});

type FixedDictFormValues = z.infer<typeof fixedDictFormSchema>;

function toFormValues(record?: SystemDictDataRecord | null): FixedDictFormValues {
  return {
    dictLabel: record?.dictLabel ?? "",
    dictValue: record?.dictValue ?? "",
    sortOrder: record?.sortOrder ?? 0,
    remark: record?.remark ?? "",
  };
}

function buildPayload(values: FixedDictFormValues, dictTypeId: number) {
  return {
    dictTypeId,
    dictLabel: values.dictLabel.trim(),
    dictValue: values.dictValue.trim(),
    sortOrder: values.sortOrder,
    remark: values.remark?.trim() || undefined,
  };
}

type FixedDictFormDialogProps = {
  open: boolean;
  title: string;
  labelName: string;
  valueName: string;
  valueHelp?: string;
  mode: FormMode;
  record: SystemDictDataRecord | null;
  submitting: boolean;
  onSubmit: (values: FixedDictFormValues) => void;
  onCancel: () => void;
};

function FixedDictFormDialog({
  open,
  title,
  labelName,
  valueName,
  valueHelp,
  mode,
  record,
  submitting,
  onSubmit,
  onCancel,
}: FixedDictFormDialogProps) {
  const form = useForm<FixedDictFormValues>({
    resolver: zodResolver(fixedDictFormSchema),
    defaultValues: toFormValues(),
  });

  useEffect(() => {
    if (!open) return;
    form.reset(toFormValues(record));
  }, [form, open, record]);

  if (!open || typeof document === "undefined") return null;

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 px-4 py-6"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !submitting) onCancel();
      }}
    >
      <section
        className="w-full max-w-[680px] rounded-admin border border-border bg-surface shadow-admin"
        role="dialog"
        aria-modal="true"
        aria-labelledby="fixed-dict-form-title"
      >
        <header className="flex items-start justify-between gap-4 border-b border-border px-5 py-4">
          <div>
            <h2
              id="fixed-dict-form-title"
              className="text-base font-semibold text-text-primary"
            >
              {mode === "edit" ? `编辑${title}` : `新增${title}`}
            </h2>
          </div>
          <Button
            size="icon"
            variant="ghost"
            className="h-8 w-8 shrink-0"
            disabled={submitting}
            onClick={onCancel}
            aria-label="关闭表单"
          >
            <X className="h-4 w-4" aria-hidden />
          </Button>
        </header>

        <form
          className="grid gap-4 px-5 py-5 md:grid-cols-2"
          onSubmit={form.handleSubmit(onSubmit)}
        >
          <Field
            label={labelName}
            htmlFor="fixed-dict-label"
            required
            error={form.formState.errors.dictLabel?.message}
          >
            <Input
              id="fixed-dict-label"
              {...form.register("dictLabel")}
              placeholder={`请输入${labelName}`}
            />
          </Field>

          <Field
            label={valueName}
            htmlFor="fixed-dict-value"
            required
            error={form.formState.errors.dictValue?.message}
            help={valueHelp}
          >
            <Input
              id="fixed-dict-value"
              {...form.register("dictValue")}
              placeholder={`请输入${valueName}`}
            />
          </Field>

          <Field
            label="排序"
            htmlFor="fixed-dict-sort"
            required
            error={form.formState.errors.sortOrder?.message}
          >
            <Input
              id="fixed-dict-sort"
              type="number"
              min={0}
              {...form.register("sortOrder", { valueAsNumber: true })}
            />
          </Field>

          <div className="md:col-span-2">
            <Field
              label="备注"
              htmlFor="fixed-dict-remark"
              error={form.formState.errors.remark?.message}
            >
              <Textarea
                id="fixed-dict-remark"
                {...form.register("remark")}
                placeholder="可填写补充说明"
              />
            </Field>
          </div>

          <div className="md:col-span-2 flex justify-end gap-2 border-t border-border pt-4">
            <Button
              type="button"
              variant="secondary"
              disabled={submitting}
              onClick={onCancel}
            >
              取消
            </Button>
            <Button type="submit" variant="primary" disabled={submitting}>
              {submitting ? "保存中..." : "保存"}
            </Button>
          </div>
        </form>
      </section>
    </div>,
    document.body,
  );
}

export function FixedDictPage({
  title,
  description,
  dictCode,
  sortOrder = 0,
  labelName,
  valueName,
  valueHelp,
}: FixedDictPageProps) {
  const [dictTypeId, setDictTypeId] = useState<number | null>(null);
  const [dictTypeLoading, setDictTypeLoading] = useState(true);
  const [formOpen, setFormOpen] = useState(false);
  const [formMode, setFormMode] = useState<FormMode>("create");
  const [editingRecord, setEditingRecord] = useState<SystemDictDataRecord | null>(null);
  const [formSubmitting, setFormSubmitting] = useState(false);
  const [deletingRecord, setDeletingRecord] = useState<SystemDictDataRecord | null>(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const loadedDictTypeRef = useRef<number | null>(null);

  useEffect(() => {
    let active = true;
    setDictTypeLoading(true);

    async function ensureDictType() {
      const findExactTypeId = async () => {
        const result = await getDictTypePage({ dictCode, page: 1, pageSize: 50 });
        return result.records.find((item) => item.dictCode === dictCode)?.id ?? null;
      };

      let typeId = await findExactTypeId();
      if (typeId != null) {
        return typeId;
      }

      try {
        await createDictType({
          dictName: title,
          dictCode,
          status: 1,
          sortOrder,
          remark: description.slice(0, 200),
        });
      } catch (err) {
        typeId = await findExactTypeId();
        if (typeId != null) {
          return typeId;
        }
        throw err;
      }

      return findExactTypeId();
    }

    void ensureDictType()
      .then((typeId) => {
        if (!active) return;
        setDictTypeId(typeId);
      })
      .catch((err) => {
        if (!active) return;
        toast.error({
          title: "字典类型初始化失败",
          description: getErrorMessage(err, `无法初始化 ${dictCode}`),
        });
      })
      .finally(() => {
        if (active) setDictTypeLoading(false);
      });

    return () => {
      active = false;
    };
  }, [description, dictCode, sortOrder, title]);

  const {
    data,
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
    reload,
  } = useListPage<
    FixedDictFilterState,
    SystemDictDataRecord,
    DictDataListQuery
  >({
    fetch: (query) => {
      if (!dictTypeId) {
        return Promise.resolve({
          records: [],
          total: 0,
          page: query.page ?? 1,
          pageSize: query.pageSize ?? 10,
        });
      }
      return getDictDataPage(query);
    },
    defaultFilters: DEFAULT_FILTERS,
    toQuery: (f, p, ps) => ({
      page: p,
      pageSize: ps,
      dictTypeId: dictTypeId ?? undefined,
      dictLabel: f.dictLabel.trim() || undefined,
      dictValue: f.dictValue.trim() || undefined,
    }),
    onError: (err) =>
      toast.error({
        title: "加载失败",
        description: getErrorMessage(err, `${title}加载失败`),
      }),
  });

  useEffect(() => {
    if (!dictTypeId || loadedDictTypeRef.current === dictTypeId) return;
    loadedDictTypeRef.current = dictTypeId;
    void reload();
  }, [dictTypeId, reload]);

  const openCreateForm = () => {
    setFormMode("create");
    setEditingRecord(null);
    setFormOpen(true);
  };

  const openEditForm = (record: SystemDictDataRecord) => {
    setFormMode("edit");
    setEditingRecord(record);
    setFormOpen(true);
  };

  const submitForm = async (values: FixedDictFormValues) => {
    if (!dictTypeId) return;
    setFormSubmitting(true);
    try {
      if (formMode === "edit" && editingRecord) {
        await updateDictData(editingRecord.id, buildPayload(values, dictTypeId));
        toast.success(`${title}已更新`);
      } else {
        await createDictData(buildPayload(values, dictTypeId));
        toast.success(`${title}已创建`);
      }
      clearDictItemCache(dictCode);
      setFormOpen(false);
      await reload();
    } catch (err) {
      toast.error({
        title: "保存失败",
        description: getErrorMessage(err, "请检查表单后重试"),
      });
    } finally {
      setFormSubmitting(false);
    }
  };

  const confirmDelete = async () => {
    if (!deletingRecord) return;
    setDeleteLoading(true);
    try {
      await deleteDictData(deletingRecord.id);
      clearDictItemCache(dictCode);
      toast.success(`${title}已删除`);
      setDeletingRecord(null);
      await reload();
    } catch (err) {
      toast.error({
        title: "删除失败",
        description: getErrorMessage(err, "请稍后重试"),
      });
    } finally {
      setDeleteLoading(false);
    }
  };

  const columns = useMemo<DataTableColumn<SystemDictDataRecord>[]>(
    () => [
      {
        title: labelName,
        dataIndex: "dictLabel",
        width: 180,
        render: (value) => <span className="font-medium">{String(value ?? "-")}</span>,
      },
      {
        title: valueName,
        dataIndex: "dictValue",
        width: 180,
        render: (value) => (
          <span className="font-mono tabular-nums text-text-secondary">
            {String(value ?? "-")}
          </span>
        ),
      },
      {
        title: "排序",
        dataIndex: "sortOrder",
        width: 90,
      },
      {
        title: "备注",
        dataIndex: "remark",
        width: 220,
        render: (value) => (
          <span className="block max-w-[220px] truncate text-text-secondary">
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
        title: "操作",
        key: "actions",
        align: "center",
        width: 170,
        render: (_, record) => (
          <div className="inline-flex items-center justify-center gap-1">
            <Button size="sm" variant="ghost" onClick={() => openEditForm(record)}>
              <Pencil className="h-4 w-4" aria-hidden />
              编辑
            </Button>
            <Button
              size="sm"
              variant="ghost"
              className="text-error hover:text-error"
              onClick={() => setDeletingRecord(record)}
            >
              <Trash2 className="h-4 w-4" aria-hidden />
              删除
            </Button>
          </div>
        ),
      },
    ],
    [labelName, valueName],
  );

  const disabled = dictTypeLoading || !dictTypeId;

  return (
    <div>
      <PageHeader title={title} description={description} />

      <SearchFilterBar
        actions={
          <>
            <Button variant="secondary" disabled={disabled} onClick={submitFilters}>
              <Search className="h-4 w-4" aria-hidden />
              查询
            </Button>
            <Button variant="secondary" disabled={disabled} onClick={resetFilters}>
              <RotateCcw className="h-4 w-4" aria-hidden />
              重置
            </Button>
            <Button variant="primary" disabled={disabled} onClick={openCreateForm}>
              <Plus className="h-4 w-4" aria-hidden />
              新增
            </Button>
          </>
        }
      >
        <Input
          placeholder={labelName}
          value={filters.dictLabel}
          disabled={disabled}
          onChange={(event) => setFilter("dictLabel", event.target.value)}
        />
        <Input
          placeholder={valueName}
          value={filters.dictValue}
          disabled={disabled}
          onChange={(event) => setFilter("dictValue", event.target.value)}
        />
      </SearchFilterBar>

      <div className="rounded-admin border border-border bg-surface shadow-admin">
        <TableToolbar
          title={title}
          description={dictTypeId ? `字典编码：${dictCode}` : "字典类型未初始化"}
          actions={
            <Button variant="secondary" disabled={disabled} onClick={reload}>
              <RefreshCw className="h-4 w-4" aria-hidden />
              刷新
            </Button>
          }
        />
        <DataTable
          columns={columns}
          dataSource={data}
          rowKey="id"
          loading={loading || dictTypeLoading}
          error={error}
          minWidth={980}
          empty={
            <EmptyState
              title={`暂无${title}`}
              description="请新增字典项，或调整筛选条件后重试。"
              actionText={disabled ? undefined : "新增"}
              onAction={disabled ? undefined : openCreateForm}
            />
          }
        />
        <Pagination
          page={page}
          pageSize={pageSize}
          total={total}
          disabled={loading || disabled}
          onPageChange={setPage}
          onPageSizeChange={setPageSize}
        />
      </div>

      <FixedDictFormDialog
        open={formOpen}
        title={title}
        labelName={labelName}
        valueName={valueName}
        valueHelp={valueHelp}
        mode={formMode}
        record={editingRecord}
        submitting={formSubmitting}
        onSubmit={submitForm}
        onCancel={() => setFormOpen(false)}
      />

      <ConfirmDialog
        open={Boolean(deletingRecord)}
        title={`删除${title}`}
        description={`确认删除「${deletingRecord?.dictLabel ?? ""}」吗？`}
        confirmText="删除"
        danger
        loading={deleteLoading}
        onConfirm={confirmDelete}
        onCancel={() => setDeletingRecord(null)}
      />
    </div>
  );
}
