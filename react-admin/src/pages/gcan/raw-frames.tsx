import { Copy, RefreshCw, RotateCcw, Search } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  getGcanRawFrameCurrent,
  getGcanRawFrameCurrentTable,
  getGcanRawFrameHistoryPage,
} from "@/api/gcan";
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
import { DICT_CODES } from "@/constants/dicts";
import { useDictOptions } from "@/hooks/use-dict-options";
import { formatDateTime } from "@/lib/datetime";
import { getErrorMessage } from "@/lib/api-error";
import type {
  DataTableColumn,
  GcanByteFormat,
  GcanRawFrameQuery,
  GcanRawFrameRecord,
} from "@/types";

const BYTE_FORMATS: Array<{ value: GcanByteFormat; label: string }> = [
  { value: "hex", label: "HEX" },
  { value: "bin", label: "BIN" },
  { value: "decimal", label: "DEC" },
];

type RawFrameMode = "current" | "history";

type RawFrameFilterState = {
  vehicleName: string;
  mineId: string;
  vehicleType: string;
  boxIdHex: string;
  canId: string;
  receivedStart: string;
  receivedEnd: string;
};

const DEFAULT_FILTERS: RawFrameFilterState = {
  vehicleName: "",
  mineId: "",
  vehicleType: "",
  boxIdHex: "",
  canId: "",
  receivedStart: "",
  receivedEnd: "",
};

function formatBytes(data: string[]) {
  return data.length > 0 ? data.join(" ") : "-";
}

function toLocalInputValue(date: Date) {
  const pad = (value: number) => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function defaultHistoryStart() {
  const date = new Date();
  date.setHours(date.getHours() - 1);
  return toLocalInputValue(date);
}

function buildFrameQuery(filters: RawFrameFilterState): GcanRawFrameQuery {
  return {
    vehicleName: filters.vehicleName.trim() || undefined,
    mineId: filters.mineId.trim() || undefined,
    vehicleType: filters.vehicleType.trim() || undefined,
    boxIdHex: filters.boxIdHex.trim() || undefined,
    canId: filters.canId.trim() || undefined,
  };
}

function createFrameColumns(
  mineLabelMap: Map<string, string>,
  vehicleTypeLabelMap: Map<string, string>,
): DataTableColumn<GcanRawFrameRecord>[] {
  return [
    {
      title: "车辆名称",
      dataIndex: "vehicleName",
      width: 150,
      render: (value) => <span className="font-medium">{String(value || "-")}</span>,
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
      title: "车型",
      dataIndex: "vehicleType",
      width: 140,
      render: (value) => {
        const vehicleType = String(value ?? "");
        const label = vehicleTypeLabelMap.get(vehicleType) ?? vehicleType;
        return <span className="text-text-secondary">{label || "-"}</span>;
      },
    },
    {
      title: "盒子 ID",
      dataIndex: "boxIdHex",
      width: 120,
      render: (value, record) => (
        <span className="font-mono tabular-nums text-text-secondary">
          <span title="盒子 ID(HEX)">{String(value ?? "-")}</span>
          <span className="ml-2 text-xs text-text-tertiary" title="盒子 ID(DEC)">
            ({record.boxIdDec})
          </span>
        </span>
      ),
    },
    {
      title: "CAN ID",
      dataIndex: "canId",
      width: 120,
      render: (value) => <span className="font-mono">{String(value ?? "-")}</span>,
    },
    {
      title: "数据字节",
      dataIndex: "data",
      width: 360,
      render: (value) => (
        <span className="block max-w-[360px] break-all font-mono text-[13px] text-text-secondary">
          {Array.isArray(value) ? formatBytes(value) : "-"}
        </span>
      ),
    },
    {
      title: "接收时间",
      dataIndex: "receivedAt",
      width: 180,
      render: (value) => (
        <span className="whitespace-nowrap tabular-nums">
          {formatDateTime(String(value ?? ""))}
        </span>
      ),
    },
  ];
}

async function copyText(text: string) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text);
    return;
  }

  const textarea = document.createElement("textarea");
  textarea.value = text;
  textarea.setAttribute("readonly", "true");
  textarea.style.position = "absolute";
  textarea.style.left = "-9999px";
  document.body.appendChild(textarea);
  textarea.select();
  document.execCommand("copy");
  document.body.removeChild(textarea);
}

export function GcanRawFramesPage() {
  const [mode, setMode] = useState<RawFrameMode>("current");
  const [format, setFormat] = useState<GcanByteFormat>("hex");
  const [filters, setFilters] = useState<RawFrameFilterState>(DEFAULT_FILTERS);
  const [appliedFilters, setAppliedFilters] = useState<RawFrameFilterState>(DEFAULT_FILTERS);
  const [frames, setFrames] = useState<GcanRawFrameRecord[]>([]);
  const [framesLoading, setFramesLoading] = useState(false);
  const [framesError, setFramesError] = useState("");
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [tableText, setTableText] = useState("");
  const [tableLoading, setTableLoading] = useState(false);
  const [tableError, setTableError] = useState("");
  const [copying, setCopying] = useState(false);
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

  const loadFrames = useCallback(async () => {
    setFramesLoading(true);
    setFramesError("");

    try {
      if (mode === "history") {
        const result = await getGcanRawFrameHistoryPage(
          {
            ...buildFrameQuery(appliedFilters),
            receivedStart: appliedFilters.receivedStart || defaultHistoryStart(),
            receivedEnd: appliedFilters.receivedEnd || undefined,
            page,
            pageSize,
          },
          format,
        );
        setFrames(result.records);
        setTotal(result.total);
      } else {
        const data = await getGcanRawFrameCurrent(buildFrameQuery(appliedFilters), format);
        setFrames(data);
        setTotal(data.length);
      }
    } catch (loadError) {
      setFrames([]);
      setTotal(0);
      setFramesError(getErrorMessage(loadError, "原始 CAN 帧加载失败"));
      toast.error({
        title: "加载失败",
        description: getErrorMessage(loadError, "原始 CAN 帧加载失败"),
      });
    } finally {
      setFramesLoading(false);
    }
  }, [appliedFilters, format, mode, page, pageSize]);

  const loadTable = useCallback(async () => {
    setTableLoading(true);
    setTableError("");

    try {
      const data = await getGcanRawFrameCurrentTable(buildFrameQuery(appliedFilters), format);
      setTableText(data);
    } catch (loadError) {
      setTableText("");
      setTableError(getErrorMessage(loadError, "原始 CAN 帧文本表格加载失败"));
      toast.error({
        title: "表格加载失败",
        description: getErrorMessage(loadError, "原始 CAN 帧文本表格加载失败"),
      });
    } finally {
      setTableLoading(false);
    }
  }, [appliedFilters, format]);

  const reload = useCallback(() => {
    void loadFrames();
    if (mode === "current") {
      void loadTable();
    }
  }, [loadFrames, loadTable, mode]);

  useEffect(() => {
    reload();
  }, [reload]);

  const columns = useMemo(
    () => createFrameColumns(mineLabelMap, vehicleTypeLabelMap),
    [mineLabelMap, vehicleTypeLabelMap],
  );

  const setFilter = <K extends keyof RawFrameFilterState>(
    key: K,
    value: RawFrameFilterState[K],
  ) => {
    setFilters((current) => ({ ...current, [key]: value }));
  };

  const submitFilters = () => {
    setPage(1);
    setAppliedFilters(filters);
  };

  const resetFilters = () => {
    setPage(1);
    setFilters(DEFAULT_FILTERS);
    setAppliedFilters(DEFAULT_FILTERS);
  };

  const handleModeChange = (nextMode: RawFrameMode) => {
    setMode(nextMode);
    setPage(1);
    if (nextMode === "history") {
      setTableText("");
      setTableError("");
    }
  };

  const handleCopy = async () => {
    if (!tableText) return;

    setCopying(true);
    try {
      await copyText(tableText);
      toast.success("文本表格已复制");
    } catch (copyError) {
      toast.error({
        title: "复制失败",
        description: getErrorMessage(copyError, "无法复制文本表格"),
      });
    } finally {
      setCopying(false);
    }
  };

  return (
    <div>
      <PageHeader
        title="原始 CAN 帧诊断"
        description="查询当前原始 CAN 帧快照，或按时间区间追溯 CAN 历史记录。"
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
        <Input
          placeholder="CAN ID"
          value={filters.canId}
          onChange={(event) => setFilter("canId", event.target.value)}
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
        {mode === "history" && (
          <>
            <Input
              type="datetime-local"
              value={filters.receivedStart}
              onChange={(event) => setFilter("receivedStart", event.target.value)}
            />
            <Input
              type="datetime-local"
              value={filters.receivedEnd}
              onChange={(event) => setFilter("receivedEnd", event.target.value)}
            />
          </>
        )}
      </SearchFilterBar>

      <div className="rounded-admin border border-border bg-surface shadow-admin">
        <TableToolbar
          title={mode === "current" ? "当前快照" : "历史记录"}
          description={
            mode === "current"
              ? `当前格式：${format.toUpperCase()}`
              : "历史模式默认查询最近 1 小时"
          }
          actions={
            <>
              <div className="inline-flex rounded-lg border border-border bg-surface p-1">
                <Button
                  size="sm"
                  variant={mode === "current" ? "primary" : "ghost"}
                  className="rounded-md"
                  onClick={() => handleModeChange("current")}
                >
                  当前快照
                </Button>
                <Button
                  size="sm"
                  variant={mode === "history" ? "primary" : "ghost"}
                  className="rounded-md"
                  onClick={() => handleModeChange("history")}
                >
                  历史记录
                </Button>
              </div>
              <div className="inline-flex rounded-lg border border-border bg-surface p-1">
                {BYTE_FORMATS.map((item) => (
                  <Button
                    key={item.value}
                    size="sm"
                    variant={format === item.value ? "primary" : "ghost"}
                    className="rounded-md"
                    onClick={() => setFormat(item.value)}
                  >
                    {item.label}
                  </Button>
                ))}
              </div>
              <Button variant="secondary" onClick={reload}>
                <RefreshCw className="h-4 w-4" aria-hidden />
                刷新
              </Button>
            </>
          }
        />

        <DataTable
          columns={columns}
          dataSource={frames}
          rowKey={(record) =>
            `${record.boxIdHex}-${record.canId}-${record.receivedAt ?? ""}`
          }
          loading={framesLoading}
          error={framesError}
          minWidth={1280}
          empty={
            <EmptyState
              title="暂无原始 CAN 帧"
              description="当前没有符合条件的原始 CAN 帧。"
              actionText="刷新"
              onAction={loadFrames}
            />
          }
        />

        {mode === "history" && (
          <Pagination
            page={page}
            pageSize={pageSize}
            total={total}
            disabled={framesLoading}
            onPageChange={setPage}
            onPageSizeChange={(size) => {
              setPageSize(size);
              setPage(1);
            }}
          />
        )}
      </div>

      {mode === "current" && (
        <div className="mt-4 rounded-admin border border-border bg-surface shadow-admin">
          <TableToolbar
            title="纯文本表格"
            description="文本表格会使用当前快照模式下的筛选条件。"
            actions={
              <>
                <Button variant="secondary" onClick={loadTable}>
                  <RefreshCw className="h-4 w-4" aria-hidden />
                  重载文本
                </Button>
                <Button
                  variant="secondary"
                  disabled={!tableText || copying}
                  onClick={handleCopy}
                >
                  <Copy className="h-4 w-4" aria-hidden />
                  {copying ? "复制中..." : "复制"}
                </Button>
              </>
            }
          />

          <div className="p-5">
            {tableLoading ? (
              <div className="rounded-lg border border-border bg-slate-50 p-4 text-sm text-text-tertiary">
                正在加载文本表格...
              </div>
            ) : tableError ? (
              <EmptyState
                title="文本表格加载失败"
                description={tableError}
                actionText="重试"
                onAction={loadTable}
              />
            ) : tableText ? (
              <pre className="max-h-[480px] overflow-auto rounded-lg border border-border bg-slate-50 p-4 font-mono text-[12px] leading-5 whitespace-pre text-text-primary">
                {tableText}
              </pre>
            ) : (
              <EmptyState
                title="暂无文本表格"
                description="切换格式或刷新后再查看。"
                actionText="重载文本"
                onAction={loadTable}
              />
            )}
          </div>
        </div>
      )}
    </div>
  );
}
