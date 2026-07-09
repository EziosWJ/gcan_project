import { zodResolver } from "@hookform/resolvers/zod";
import { X } from "lucide-react";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { createPortal } from "react-dom";
import { Field } from "@/components/common/field";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import type { GcanVehicleRecord, GcanVehicleTypeRecord } from "@/types";
import {
  vehicleFormSchema,
  toVehicleFormValues,
  type VehicleFormMode,
  type VehicleFormValues,
} from "./schema";

function formatBoxIdDec(value: string) {
  const normalized = value.trim().replace(/^0x/i, "");
  if (!/^[0-9a-fA-F]{1,2}$/.test(normalized)) {
    return "-";
  }
  return String(Number.parseInt(normalized, 16));
}

type VehicleFormDialogProps = {
  open: boolean;
  mode: VehicleFormMode;
  record: GcanVehicleRecord | null;
  vehicleTypes: GcanVehicleTypeRecord[];
  submitting: boolean;
  onSubmit: (values: VehicleFormValues) => void;
  onCancel: () => void;
};

export function VehicleFormDialog({
  open,
  mode,
  record,
  vehicleTypes,
  submitting,
  onSubmit,
  onCancel,
}: VehicleFormDialogProps) {
  const form = useForm<VehicleFormValues>({
    resolver: zodResolver(vehicleFormSchema),
    defaultValues: toVehicleFormValues(),
  });

  useEffect(() => {
    if (!open) return;
    form.reset(toVehicleFormValues(record ?? undefined));
  }, [form, open, record]);

  const boxIdDec = formatBoxIdDec(form.watch("boxIdHex") ?? "");

  if (!open || typeof document === "undefined") return null;

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 px-4 py-6"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !submitting) {
          onCancel();
        }
      }}
    >
      <section
        className="w-full max-w-[720px] rounded-admin border border-border bg-surface shadow-admin"
        role="dialog"
        aria-modal="true"
        aria-labelledby="gcan-vehicle-form-title"
      >
        <header className="flex items-start justify-between gap-4 border-b border-border px-5 py-4">
          <div>
            <h2
              id="gcan-vehicle-form-title"
              className="text-base font-semibold text-text-primary"
            >
              {mode === "edit" ? "编辑车辆档案" : "新增车辆档案"}
            </h2>
            <p className="mt-1 text-[13px] text-text-tertiary">
              {mode === "edit" ? "更新当前车辆基础信息" : "创建一个新的车辆档案"}
            </p>
          </div>
          <Button
            size="icon"
            variant="ghost"
            className="h-8 w-8 shrink-0"
            disabled={submitting}
            onClick={onCancel}
            aria-label="关闭车辆表单"
          >
            <X className="h-4 w-4" aria-hidden />
          </Button>
        </header>

        <form
          className="grid gap-4 px-5 py-5 md:grid-cols-2"
          onSubmit={form.handleSubmit(onSubmit)}
        >
          <Field
            label="车辆名称"
            htmlFor="gcan-vehicle-name"
            required
            error={form.formState.errors.vehicleName?.message}
          >
            <Input
              id="gcan-vehicle-name"
              {...form.register("vehicleName")}
              placeholder="请输入车辆名称"
            />
          </Field>

          <Field
            label="车辆类型"
            htmlFor="gcan-vehicle-type"
            required
            error={form.formState.errors.vehicleType?.message}
          >
            <Select id="gcan-vehicle-type" {...form.register("vehicleType")}>
              <option value="">请选择车辆类型</option>
              {vehicleTypes.map((item) => (
                <option key={item.code} value={item.code}>
                  {item.label}
                </option>
              ))}
            </Select>
          </Field>

          <Field
            label="盒子 ID(HEX)"
            htmlFor="gcan-box-id-hex"
            required
            error={form.formState.errors.boxIdHex?.message}
            help="支持 0x01 或 01，保存时会转成大写"
          >
            <Input
              id="gcan-box-id-hex"
              {...form.register("boxIdHex")}
              placeholder="例如 01"
            />
          </Field>

          <Field label="盒子 ID(DEC)" htmlFor="gcan-box-id-dec">
            <Input id="gcan-box-id-dec" value={boxIdDec} readOnly />
          </Field>

          <Field
            label="状态"
            htmlFor="gcan-status"
            required
            error={form.formState.errors.status?.message}
          >
            <Select
              id="gcan-status"
              {...form.register("status", { valueAsNumber: true })}
            >
              <option value={1}>启用</option>
              <option value={0}>禁用</option>
            </Select>
          </Field>

          <div className="md:col-span-2">
            <Field
              label="备注"
              htmlFor="gcan-remark"
              error={form.formState.errors.remark?.message}
            >
              <Textarea
                id="gcan-remark"
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
