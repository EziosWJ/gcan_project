import { Calculator, Copy, RefreshCw } from "lucide-react";
import { useMemo, useState } from "react";
import { Field } from "@/components/common/field";
import { PageHeader } from "@/components/common/page-header";
import { TableToolbar } from "@/components/common/table-toolbar";
import { toast } from "@/components/common/toast-store";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

const DEFAULT_HEX = "00";
const DEFAULT_DEC = "0";

type ParseResult =
  | {
      kind: "empty";
    }
  | {
      kind: "error";
      message: string;
    }
  | {
      kind: "ok";
      value: number;
    };

function copyText(text: string) {
  if (navigator.clipboard?.writeText) {
    return navigator.clipboard.writeText(text);
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
  return Promise.resolve();
}

function normalizeHexInput(value: string) {
  return value.trim().replace(/\s+/g, "");
}

function formatHex(value: number) {
  return value.toString(16).toUpperCase().padStart(2, "0");
}

function parseHex(value: string): ParseResult {
  const normalized = normalizeHexInput(value).replace(/^0x/i, "");

  if (normalized === "") {
    return { kind: "empty" };
  }

  if (!/^[0-9a-fA-F]+$/.test(normalized)) {
    return {
      kind: "error",
      message: "HEX 仅支持 0-9、A-F，以及可选的 0x 前缀。",
    };
  }

  const parsed = Number.parseInt(normalized, 16);

  if (!Number.isFinite(parsed) || parsed < 0 || parsed > 255) {
    return {
      kind: "error",
      message: "盒子 ID 模式下 HEX 必须在 00-FF 范围内。",
    };
  }

  return { kind: "ok", value: parsed };
}

function parseDec(value: string): ParseResult {
  const normalized = value.trim();

  if (normalized === "") {
    return { kind: "empty" };
  }

  if (!/^[0-9]+$/.test(normalized)) {
    return {
      kind: "error",
      message: "DEC 仅支持十进制数字。",
    };
  }

  const parsed = Number.parseInt(normalized, 10);

  if (!Number.isFinite(parsed) || parsed < 0 || parsed > 255) {
    return {
      kind: "error",
      message: "盒子 ID 模式下 DEC 必须在 0-255 范围内。",
    };
  }

  return { kind: "ok", value: parsed };
}

export function GcanHexDecConverterPage() {
  const [hexInput, setHexInput] = useState(DEFAULT_HEX);
  const [decInput, setDecInput] = useState(DEFAULT_DEC);
  const [hexError, setHexError] = useState("");
  const [decError, setDecError] = useState("");
  const [copying, setCopying] = useState<"hex" | "dec" | null>(null);

  const hexResult = useMemo(() => parseHex(hexInput), [hexInput]);
  const decResult = useMemo(() => parseDec(decInput), [decInput]);

  const currentValue =
    hexResult.kind === "ok"
      ? hexResult.value
      : decResult.kind === "ok"
        ? decResult.value
        : null;

  const hexCopyValue = currentValue === null ? "" : formatHex(currentValue);
  const decCopyValue = currentValue === null ? "" : String(currentValue);

  const handleHexChange = (value: string) => {
    const nextValue = normalizeHexInput(value);
    setHexInput(nextValue);

    const result = parseHex(nextValue);
    if (result.kind === "ok") {
      setHexError("");
      setDecInput(String(result.value));
      setDecError("");
      return;
    }

    if (result.kind === "empty") {
      setHexError("");
      setDecInput("");
      setDecError("");
      return;
    }

    setHexError(result.message);
    setDecInput("");
    setDecError("");
  };

  const handleDecChange = (value: string) => {
    const nextValue = value.trim();
    setDecInput(nextValue);

    const result = parseDec(nextValue);
    if (result.kind === "ok") {
      setDecError("");
      setHexInput(formatHex(result.value));
      setHexError("");
      return;
    }

    if (result.kind === "empty") {
      setDecError("");
      setHexInput("");
      setHexError("");
      return;
    }

    setDecError(result.message);
    setHexInput("");
    setHexError("");
  };

  const handleReset = () => {
    setHexInput(DEFAULT_HEX);
    setDecInput(DEFAULT_DEC);
    setHexError("");
    setDecError("");
  };

  const handleCopy = async (target: "hex" | "dec") => {
    const text = target === "hex" ? hexCopyValue : decCopyValue;
    if (!text) return;

    setCopying(target);
    try {
      await copyText(text);
      toast.success(target === "hex" ? "HEX 已复制" : "DEC 已复制");
    } catch {
      toast.error(target === "hex" ? "HEX 复制失败" : "DEC 复制失败");
    } finally {
      setCopying(null);
    }
  };

  const handleHexBlur = () => {
    if (hexResult.kind === "ok") {
      setHexInput(formatHex(hexResult.value));
    }
  };

  const handleDecBlur = () => {
    if (decResult.kind === "ok") {
      setDecInput(String(decResult.value));
    }
  };

  return (
    <div>
      <PageHeader
        title="HEX/DEC 转换"
        description="默认使用盒子 ID 模式，支持 HEX 与 DEC 双向同步，复制结果会统一成规范格式。"
      />

      <div className="rounded-admin border border-border bg-surface shadow-admin">
        <TableToolbar
          title="转换器"
          description="HEX 支持 3E、3e、0x3E；DEC 仅接受十进制数字。盒子 ID 模式下范围限制为 0-255。"
          actions={
            <>
              <div className="inline-flex items-center gap-2 rounded-lg border border-border bg-slate-50 px-3 py-2 text-xs font-medium text-text-secondary">
                <Calculator className="h-3.5 w-3.5" aria-hidden />
                盒子 ID 模式
              </div>
              <Button variant="secondary" onClick={handleReset}>
                <RefreshCw className="h-4 w-4" aria-hidden />
                重置
              </Button>
            </>
          }
        />

        <div className="grid gap-5 p-5 lg:grid-cols-2">
          <Field
            label="HEX 输入"
            htmlFor="gcan-hex-input"
            help="支持 3E、3e、0x3E；输入后会自动同步 DEC。"
            error={hexError}
          >
            <div className="flex flex-col gap-2 sm:flex-row">
              <Input
                id="gcan-hex-input"
                value={hexInput}
                onChange={(event) => handleHexChange(event.target.value)}
                onBlur={handleHexBlur}
                placeholder="例如 3E 或 0x3E"
                autoComplete="off"
                spellCheck={false}
                className="min-w-0 flex-1 font-mono uppercase tabular-nums tracking-[0.08em]"
              />
              <Button
                variant="secondary"
                onClick={() => void handleCopy("hex")}
                disabled={!hexCopyValue || copying === "hex"}
                className="sm:shrink-0"
              >
                <Copy className="h-4 w-4" aria-hidden />
                {copying === "hex" ? "复制中..." : "复制 HEX"}
              </Button>
            </div>
          </Field>

          <Field
            label="DEC 输入"
            htmlFor="gcan-dec-input"
            help="只接受十进制数字；输入后会自动同步 HEX。"
            error={decError}
          >
            <div className="flex flex-col gap-2 sm:flex-row">
              <Input
                id="gcan-dec-input"
                value={decInput}
                onChange={(event) => handleDecChange(event.target.value)}
                onBlur={handleDecBlur}
                placeholder="例如 62"
                autoComplete="off"
                inputMode="numeric"
                pattern="[0-9]*"
                spellCheck={false}
                className="min-w-0 flex-1 font-mono tabular-nums"
              />
              <Button
                variant="secondary"
                onClick={() => void handleCopy("dec")}
                disabled={!decCopyValue || copying === "dec"}
                className="sm:shrink-0"
              >
                <Copy className="h-4 w-4" aria-hidden />
                {copying === "dec" ? "复制中..." : "复制 DEC"}
              </Button>
            </div>
          </Field>

          <div className="lg:col-span-2 rounded-lg border border-border bg-slate-50 p-4">
            <div className="flex flex-wrap items-center gap-3 text-sm">
              <span className="font-medium text-text-primary">当前同步结果</span>
              <span className="font-mono text-text-secondary">
                HEX：{hexCopyValue || "-"}
              </span>
              <span className="font-mono text-text-secondary">
                DEC：{decCopyValue || "-"}
              </span>
            </div>
            <p className="mt-2 text-xs text-text-tertiary">
              复制按钮会输出规范化后的值：HEX 始终大写，盒子 ID 模式下固定两位；DEC 始终为十进制数字。
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
