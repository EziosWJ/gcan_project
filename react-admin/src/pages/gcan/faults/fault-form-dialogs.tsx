import { zodResolver } from "@hookform/resolvers/zod";
import { X } from "lucide-react";
import { useEffect } from "react";
import { createPortal } from "react-dom";
import { useForm } from "react-hook-form";
import type { ReactNode } from "react";
import { z } from "zod";
import type { GcanFaultDefinitionRecord, GcanFaultProfileRecord } from "@/types/gcan-fault";
import { Field } from "@/components/common/field";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";

type FormMode = "create" | "edit";

const profileSchema = z.object({
  profileCode: z.string().trim().min(1, "故障表编码不能为空").max(100, "不能超过 100 个字符"),
  profileName: z.string().trim().min(1, "故障表名称不能为空").max(100, "不能超过 100 个字符"),
  manufacturer: z.string().trim().max(100, "不能超过 100 个字符").optional(),
  vehicleType: z.string().trim().max(100, "不能超过 100 个字符").optional(),
  protocolVersion: z.string().trim().max(100, "不能超过 100 个字符").optional(),
  applicableVehicleDescription: z.string().trim().max(500, "不能超过 500 个字符").optional(),
  status: z.coerce.number().pipe(z.union([z.literal(0), z.literal(1)])),
  remark: z.string().trim().max(500, "不能超过 500 个字符").optional(),
});

const definitionSchema = z.object({
  profileCode: z.string().trim().min(1, "故障表编码不能为空").max(100, "不能超过 100 个字符"),
  faultCode: z.string().trim().min(1, "故障码不能为空").max(100, "不能超过 100 个字符"),
  rawLevelCode: z.string().trim().max(50, "不能超过 50 个字符").optional(),
  rawLevelName: z.string().trim().max(100, "不能超过 100 个字符").optional(),
  faultName: z.string().trim().max(200, "不能超过 200 个字符").optional(),
  faultDefinition: z.string().trim().max(2000, "不能超过 2000 个字符").optional(),
  analysis: z.string().trim().max(2000, "不能超过 2000 个字符").optional(),
  symptom: z.string().trim().max(2000, "不能超过 2000 个字符").optional(),
  recovery: z.string().trim().max(2000, "不能超过 2000 个字符").optional(),
  removal: z.string().trim().max(2000, "不能超过 2000 个字符").optional(),
  handlingSuggestion: z.string().trim().max(2000, "不能超过 2000 个字符").optional(),
  remark: z.string().trim().max(500, "不能超过 500 个字符").optional(),
  status: z.coerce.number().pipe(z.union([z.literal(0), z.literal(1)])),
});

export type ProfileFormValues = z.infer<typeof profileSchema>;
export type DefinitionFormValues = z.infer<typeof definitionSchema>;

function profileValues(record?: GcanFaultProfileRecord | null): ProfileFormValues {
  return {
    profileCode: record?.profileCode ?? "",
    profileName: record?.profileName ?? "",
    manufacturer: record?.manufacturer ?? "",
    vehicleType: record?.vehicleType ?? "",
    protocolVersion: record?.protocolVersion ?? "",
    applicableVehicleDescription: record?.applicableVehicleDescription ?? "",
    status: record?.status ?? 1,
    remark: record?.remark ?? "",
  };
}

function definitionValues(record?: GcanFaultDefinitionRecord | null): DefinitionFormValues {
  return {
    profileCode: record?.profileCode ?? "",
    faultCode: record?.faultCode ?? "",
    rawLevelCode: record?.rawLevelCode ?? "",
    rawLevelName: record?.rawLevelName ?? "",
    faultName: record?.faultName ?? "",
    faultDefinition: record?.faultDefinition ?? "",
    analysis: record?.analysis ?? "",
    symptom: record?.symptom ?? "",
    recovery: record?.recovery ?? "",
    removal: record?.removal ?? "",
    handlingSuggestion: record?.handlingSuggestion ?? "",
    remark: record?.remark ?? "",
    status: record?.status ?? 1,
  };
}

type ProfileDialogProps = {
  open: boolean;
  mode: FormMode;
  record: GcanFaultProfileRecord | null;
  submitting: boolean;
  onSubmit: (values: ProfileFormValues) => void;
  onCancel: () => void;
};

export function FaultProfileFormDialog({
  open,
  mode,
  record,
  submitting,
  onSubmit,
  onCancel,
}: ProfileDialogProps) {
  const form = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: profileValues(),
  });

  useEffect(() => {
    if (open) form.reset(profileValues(record));
  }, [form, open, record]);

  if (!open || typeof document === "undefined") return null;

  return createPortal(
    <DialogShell
      title={mode === "edit" ? "编辑故障码表" : "新增故障码表"}
      description="每个车型可维护一份独立的厂家故障码对照表。"
      submitting={submitting}
      onCancel={onCancel}
    >
      <form className="grid gap-4 px-5 py-5 md:grid-cols-2" onSubmit={form.handleSubmit(onSubmit)}>
        <Field label="故障表编码" htmlFor="fault-profile-code" required error={form.formState.errors.profileCode?.message} help="建议使用车型或厂家协议的稳定编码">
          <Input id="fault-profile-code" {...form.register("profileCode")} placeholder="例如 REN_19_V1" disabled={mode === "edit"} />
        </Field>
        <Field label="故障表名称" htmlFor="fault-profile-name" required error={form.formState.errors.profileName?.message}>
          <Input id="fault-profile-name" {...form.register("profileName")} placeholder="例如 19座人车故障表" />
        </Field>
        <Field label="车辆厂家" htmlFor="fault-profile-manufacturer" error={form.formState.errors.manufacturer?.message}>
          <Input id="fault-profile-manufacturer" {...form.register("manufacturer")} placeholder="厂家名称" />
        </Field>
        <Field label="车型编码" htmlFor="fault-profile-vehicle-type" error={form.formState.errors.vehicleType?.message}>
          <Input id="fault-profile-vehicle-type" {...form.register("vehicleType")} placeholder="例如 REN_19" />
        </Field>
        <Field label="协议版本" htmlFor="fault-profile-protocol-version" error={form.formState.errors.protocolVersion?.message}>
          <Input id="fault-profile-protocol-version" {...form.register("protocolVersion")} placeholder="例如 V1.5" />
        </Field>
        <Field label="状态" htmlFor="fault-profile-status" required error={form.formState.errors.status?.message}>
          <Select id="fault-profile-status" {...form.register("status", { valueAsNumber: true })}>
            <option value={1}>启用</option>
            <option value={0}>禁用</option>
          </Select>
        </Field>
        <div className="md:col-span-2">
          <Field label="适用车辆说明" htmlFor="fault-profile-description" error={form.formState.errors.applicableVehicleDescription?.message}>
            <Textarea id="fault-profile-description" {...form.register("applicableVehicleDescription")} placeholder="补充厂家、批次或硬件版本适用范围" />
          </Field>
        </div>
        <div className="md:col-span-2">
          <Field label="备注" htmlFor="fault-profile-remark" error={form.formState.errors.remark?.message}>
            <Textarea id="fault-profile-remark" {...form.register("remark")} placeholder="可填写维护说明" />
          </Field>
        </div>
        <FormActions submitting={submitting} onCancel={onCancel} />
      </form>
    </DialogShell>,
    document.body,
  );
}

type DefinitionDialogProps = {
  open: boolean;
  mode: FormMode;
  record: GcanFaultDefinitionRecord | null;
  profileOptions: GcanFaultProfileRecord[];
  submitting: boolean;
  onSubmit: (values: DefinitionFormValues) => void;
  onCancel: () => void;
};

export function FaultDefinitionFormDialog({
  open,
  mode,
  record,
  profileOptions,
  submitting,
  onSubmit,
  onCancel,
}: DefinitionDialogProps) {
  const form = useForm<DefinitionFormValues>({
    resolver: zodResolver(definitionSchema),
    defaultValues: definitionValues(),
  });

  useEffect(() => {
    if (open) form.reset(definitionValues(record));
  }, [form, open, record]);

  if (!open || typeof document === "undefined") return null;

  return createPortal(
    <DialogShell
      title={mode === "edit" ? "编辑故障定义" : "新增故障定义"}
      description="故障码、翻译和处理建议都归属于当前故障码表。"
      submitting={submitting}
      onCancel={onCancel}
      wide
    >
      <form className="grid max-h-[76vh] gap-4 overflow-y-auto px-5 py-5 md:grid-cols-2" onSubmit={form.handleSubmit(onSubmit)}>
        <Field label="故障码表" htmlFor="fault-definition-profile" required error={form.formState.errors.profileCode?.message}>
          <Select id="fault-definition-profile" {...form.register("profileCode")} disabled={mode === "edit"}>
            <option value="">请选择故障码表</option>
            {profileOptions.map((profile) => <option key={profile.profileCode} value={profile.profileCode} disabled={profile.status !== 1 && profile.profileCode !== record?.profileCode}>{profile.profileName} · {profile.profileCode}{profile.status !== 1 ? "（停用）" : ""}</option>)}
          </Select>
        </Field>
        <Field label="故障码" htmlFor="fault-definition-code" required error={form.formState.errors.faultCode?.message}>
          <Input id="fault-definition-code" {...form.register("faultCode")} placeholder="例如 P001" disabled={mode === "edit"} />
        </Field>
        <Field label="原始等级编码" htmlFor="fault-definition-level-code" error={form.formState.errors.rawLevelCode?.message}>
          <Input id="fault-definition-level-code" {...form.register("rawLevelCode")} placeholder="厂家原始等级值" />
        </Field>
        <Field label="原始等级名称" htmlFor="fault-definition-level-name" error={form.formState.errors.rawLevelName?.message}>
          <Input id="fault-definition-level-name" {...form.register("rawLevelName")} placeholder="例如 严重" />
        </Field>
        <Field label="故障名称" htmlFor="fault-definition-name" error={form.formState.errors.faultName?.message}>
          <Input id="fault-definition-name" {...form.register("faultName")} placeholder="面向大屏和运维人员的名称" />
        </Field>
        <Field label="状态" htmlFor="fault-definition-status" required error={form.formState.errors.status?.message}>
          <Select id="fault-definition-status" {...form.register("status", { valueAsNumber: true })}>
            <option value={1}>启用</option>
            <option value={0}>禁用</option>
          </Select>
        </Field>
        <DefinitionTextarea form={form} name="faultDefinition" label="故障定义" placeholder="说明该故障码代表什么" />
        <DefinitionTextarea form={form} name="analysis" label="原因分析" placeholder="说明可能原因和排查方向" />
        <DefinitionTextarea form={form} name="symptom" label="故障现象" placeholder="车辆或仪表上会出现的现象" />
        <DefinitionTextarea form={form} name="recovery" label="恢复条件" placeholder="故障恢复或清除的条件" />
        <DefinitionTextarea form={form} name="removal" label="排除方法" placeholder="现场排查和排除步骤" />
        <DefinitionTextarea form={form} name="handlingSuggestion" label="处理建议" placeholder="给维修人员的处理建议" />
        <div className="md:col-span-2">
          <Field label="备注" htmlFor="fault-definition-remark" error={form.formState.errors.remark?.message}>
            <Textarea id="fault-definition-remark" {...form.register("remark")} placeholder="可填写厂家原文或补充说明" />
          </Field>
        </div>
        <FormActions submitting={submitting} onCancel={onCancel} />
      </form>
    </DialogShell>,
    document.body,
  );
}

type DefinitionFieldName = "faultDefinition" | "analysis" | "symptom" | "recovery" | "removal" | "handlingSuggestion";

function DefinitionTextarea({ form, name, label, placeholder }: { form: ReturnType<typeof useForm<DefinitionFormValues>>; name: DefinitionFieldName; label: string; placeholder: string }) {
  const inputId = `fault-definition-${name}`;
  return (
    <div className="md:col-span-2">
      <Field label={label} htmlFor={inputId} error={form.formState.errors[name]?.message}>
        <Textarea id={inputId} {...form.register(name)} placeholder={placeholder} />
      </Field>
    </div>
  );
}

function DialogShell({ title, description, submitting, onCancel, wide = false, children }: { title: string; description: string; submitting: boolean; onCancel: () => void; wide?: boolean; children: ReactNode }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 px-4 py-6" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget && !submitting) onCancel(); }}>
      <section className={`w-full ${wide ? "max-w-[820px]" : "max-w-[720px]"} rounded-admin border border-border bg-surface shadow-admin`} role="dialog" aria-modal="true" aria-labelledby="gcan-fault-form-title">
        <header className="flex items-start justify-between gap-4 border-b border-border px-5 py-4">
          <div>
            <h2 id="gcan-fault-form-title" className="text-base font-semibold text-text-primary">{title}</h2>
            <p className="mt-1 text-[13px] text-text-tertiary">{description}</p>
          </div>
          <Button size="icon" variant="ghost" className="h-8 w-8 shrink-0" disabled={submitting} onClick={onCancel} aria-label="关闭故障维护表单"><X className="h-4 w-4" aria-hidden /></Button>
        </header>
        {children}
      </section>
    </div>
  );
}

function FormActions({ submitting, onCancel }: { submitting: boolean; onCancel: () => void }) {
  return (
    <div className="md:col-span-2 flex justify-end gap-2 border-t border-border pt-4">
      <Button type="button" variant="secondary" disabled={submitting} onClick={onCancel}>取消</Button>
      <Button type="submit" variant="primary" disabled={submitting}>{submitting ? "保存中..." : "保存"}</Button>
    </div>
  );
}
