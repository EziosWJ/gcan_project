import { Copy, RefreshCw } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  getGcanRawFrameCurrent,
  getGcanRawFrameCurrentTable,
} from "@/api/gcan";
import { DataTable } from "@/components/common/data-table";
import { EmptyState } from "@/components/common/empty-state";
import { PageHeader } from "@/components/common/page-header";
import { TableToolbar } from "@/components/common/table-toolbar";
import { toast } from "@/components/common/toast-store";
import { Button } from "@/components/ui/button";
import { formatDateTime } from "@/lib/datetime";
import { getErrorMessage } from "@/lib/api-error";
import type { DataTableColumn, GcanByteFormat, GcanRawFrameRecord } from "@/types";

const BYTE_FORMATS: Array<{ value: GcanByteFormat; label: string }> = [
  { value: "hex", label: "HEX" },
  { value: "bin", label: "BIN" },
  { value: "decimal", label: "DEC" },
];

function formatBytes(data: string[]) {
  return data.length > 0 ? data.join(" ") : "-";
}

function createFrameColumns(): DataTableColumn<GcanRawFrameRecord>[] {
  return [
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
  const [format, setFormat] = useState<GcanByteFormat>("hex");
  const [frames, setFrames] = useState<GcanRawFrameRecord[]>([]);
  const [framesLoading, setFramesLoading] = useState(false);
  const [framesError, setFramesError] = useState("");
  const [tableText, setTableText] = useState("");
  const [tableLoading, setTableLoading] = useState(false);
  const [tableError, setTableError] = useState("");
  const [copying, setCopying] = useState(false);

  const loadFrames = useCallback(async () => {
    setFramesLoading(true);
    setFramesError("");

    try {
      const data = await getGcanRawFrameCurrent(format);
      setFrames(data);
    } catch (loadError) {
      setFrames([]);
      setFramesError(getErrorMessage(loadError, "原始 CAN 帧加载失败"));
      toast.error({
        title: "加载失败",
        description: getErrorMessage(loadError, "原始 CAN 帧加载失败"),
      });
    } finally {
      setFramesLoading(false);
    }
  }, [format]);

  const loadTable = useCallback(async () => {
    setTableLoading(true);
    setTableError("");

    try {
      const data = await getGcanRawFrameCurrentTable(format);
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
  }, [format]);

  const reload = useCallback(() => {
    void loadFrames();
    void loadTable();
  }, [loadFrames, loadTable]);

  useEffect(() => {
    reload();
  }, [reload]);

  const columns = useMemo(() => createFrameColumns(), []);

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
        description="切换字节格式查看当前原始 CAN 帧，并可请求纯文本表格后复制。"
      />

      <div className="rounded-admin border border-border bg-surface shadow-admin">
        <TableToolbar
          title="帧快照"
          description={`当前格式：${format.toUpperCase()}`}
          actions={
            <>
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
          minWidth={860}
          empty={
            <EmptyState
              title="暂无原始 CAN 帧"
              description="当前没有可展示的原始 CAN 帧快照。"
              actionText="刷新"
              onAction={loadFrames}
            />
          }
        />
      </div>

      <div className="mt-4 rounded-admin border border-border bg-surface shadow-admin">
        <TableToolbar
          title="纯文本表格"
          description="请求 /current/table 接口返回的 text/plain 内容。"
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
    </div>
  );
}
