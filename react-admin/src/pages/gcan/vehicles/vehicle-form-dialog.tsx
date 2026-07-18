import { zodResolver } from "@hookform/resolvers/zod";
import { X } from "lucide-react";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { createPortal } from "react-dom";
import { Field } from "@/components/common/field";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import type { GcanFaultProfileRecord } from "@/types/gcan-fault";
import { Textarea } from "@/components/ui/textarea";
import type { DictSelectOption } from "@/constants/dicts";
import type { GcanVehicleRecord } from "@/types";
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
  mineOptions: DictSelectOption[];
  vehicleTypeOptions: DictSelectOption[];
  faultProfiles: GcanFaultProfileRecord[];
  submitting: boolean;
  onSubmit: (values: VehicleFormValues) => void;
  onCancel: () => void;
};

export function VehicleFormDialog({
  open,
  mode,
  record,
  mineOptions,
  vehicleTypeOptions,
  faultProfiles,
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

  const accessMode = form.watch("accessMode");
  const isExternal = accessMode === "MINE_API";
  const boxIdDec = formatBoxIdDec(form.watch("boxIdHex") ?? "");
  const resolvedMineOptions = record?.mineId && !mineOptions.some((item) => String(item.value) === record.mineId)
    ? [...mineOptions, { value: record.mineId, label: record.mineId }]
    : mineOptions;
  const resolvedVehicleTypeOptions = [
    ...vehicleTypeOptions,
    ...(isExternal && !vehicleTypeOptions.some((item) => String(item.value).toUpperCase() === "EXTERNAL")
      ? [{ value: "EXTERNAL", label: "外部接口车辆" }]
      : []),
  ];

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
            label="煤矿"
            htmlFor="gcan-mine-id"
            required
            error={form.formState.errors.mineId?.message}
          >
            <Select id="gcan-mine-id" {...form.register("mineId")}>
              <option value="">请选择煤矿</option>
              {resolvedMineOptions.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </Select>
          </Field>

          <Field
            label="接入方式"
            htmlFor="gcan-access-mode"
            required
            help="编辑已有车辆时不能切换主接入方式"
          >
            <Select id="gcan-access-mode" {...form.register("accessMode")} disabled={mode === "edit"}>
              <option value="GCAN">GCAN</option>
              <option value="MINE_API">煤矿接口</option>
            </Select>
          </Field>

          {isExternal && (
            <Field
              label="外部车辆编码"
              htmlFor="gcan-external-vehicle-code"
              required
              error={form.formState.errors.externalVehicleCode?.message}
              help="外部身份由煤矿编码和该编码组成，编辑时不可修改"
            >
              <Input
                id="gcan-external-vehicle-code"
                {...form.register("externalVehicleCode")}
                readOnly={mode === "edit"}
                placeholder="例如 R101"
              />
            </Field>
          )}

          <Field
            label="车辆类型"
            htmlFor="gcan-vehicle-type"
            required
            error={form.formState.errors.vehicleType?.message}
          >
            <Select id="gcan-vehicle-type" {...form.register("vehicleType")}>
              <option value="">请选择车辆类型</option>
              {resolvedVehicleTypeOptions.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </Select>
          </Field>

          <Field
            label="故障码表"
            htmlFor="gcan-fault-profile"
            help="可暂不关联；未关联时只显示原始故障码"
          >
            <Select id="gcan-fault-profile" {...form.register("faultProfileCode")}>
              <option value="">暂不关联</option>
              {faultProfiles.map((profile) => (
                <option key={profile.profileCode} value={profile.profileCode}>
                  {profile.profileName} · {profile.profileCode}
                </option>
              ))}
            </Select>
          </Field>

          <Field
            label="盒子 ID(HEX)"
            htmlFor="gcan-box-id-hex"
            required={!isExternal}
            error={form.formState.errors.boxIdHex?.message}
            help="支持 0x01 或 01，保存时会转成大写"
          >
            <Input
              id="gcan-box-id-hex"
              {...form.register("boxIdHex")}
              disabled={isExternal}
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
