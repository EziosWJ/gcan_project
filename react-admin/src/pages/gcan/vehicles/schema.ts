import { z } from "zod";
import type { ApiStatus, GcanVehicleRecord } from "@/types";

export type VehicleFilterState = {
  vehicleName: string;
  mineId: string;
  vehicleType: string;
  boxIdHex: string;
  status: "all" | ApiStatus;
};

export type VehicleFormMode = "create" | "edit";

export const DEFAULT_FILTERS: VehicleFilterState = {
  vehicleName: "",
  mineId: "",
  vehicleType: "",
  boxIdHex: "",
  status: "all",
};

const HEX_BOX_ID_PATTERN = /^(?:0[xX])?[0-9a-fA-F]{1,2}$/;

export const vehicleFormSchema = z.object({
  vehicleName: z
    .string()
    .trim()
    .min(1, "车辆名称不能为空")
    .max(100, "车辆名称不能超过 100 个字符"),
  mineId: z.string().trim().min(1, "煤矿不能为空").max(100, "煤矿ID不能超过 100 个字符"),
  vehicleType: z.string().trim().min(1, "车辆类型不能为空"),
  faultProfileCode: z.string().trim().max(100, "故障码表编码不能超过 100 个字符").optional(),
  boxIdHex: z
    .string()
    .trim()
    .min(1, "盒子 ID 不能为空")
    .max(4, "盒子 ID 不能超过 4 个字符")
    .regex(HEX_BOX_ID_PATTERN, "盒子 ID 必须是 00-FF 十六进制"),
  status: z.coerce.number().pipe(z.union([z.literal(0), z.literal(1)])),
  remark: z.string().trim().max(500, "备注不能超过 500 个字符").optional(),
});

export type VehicleFormValues = z.infer<typeof vehicleFormSchema>;

export function buildVehicleQuery(
  filters: VehicleFilterState,
  page: number,
  pageSize: number,
) {
  return {
    page,
    pageSize,
    vehicleName: filters.vehicleName.trim() || undefined,
    mineId: filters.mineId.trim() || undefined,
    vehicleType: filters.vehicleType.trim() || undefined,
    boxIdHex: filters.boxIdHex.trim() || undefined,
    status: filters.status === "all" ? undefined : filters.status,
  };
}

export function toVehicleFormValues(vehicle?: GcanVehicleRecord): VehicleFormValues {
  return {
    vehicleName: vehicle?.vehicleName ?? "",
    mineId: vehicle?.mineId ?? "",
    vehicleType: vehicle?.vehicleType ?? "",
    faultProfileCode: vehicle?.faultProfileCode ?? "",
    boxIdHex: vehicle?.boxIdHex ?? "",
    status: vehicle?.status ?? 1,
    remark: vehicle?.remark ?? "",
  };
}

export function buildVehiclePayload(values: VehicleFormValues) {
  return {
    vehicleName: values.vehicleName.trim(),
    mineId: values.mineId.trim(),
    vehicleType: values.vehicleType.trim().toUpperCase(),
    faultProfileCode: values.faultProfileCode?.trim() || undefined,
    boxIdHex: values.boxIdHex.trim().toUpperCase(),
    status: values.status,
    remark: values.remark?.trim() || undefined,
  };
}
