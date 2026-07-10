import { DICT_CODES } from "@/constants/dicts";
import { FixedDictPage } from "./fixed-dict-page";

export function GcanVehicleTypesPage() {
  return (
    <FixedDictPage
      title="车型管理"
      description="维护车辆档案使用的车型字典项，字典项值会作为协议解析匹配编码。"
      dictCode={DICT_CODES.GCAN_VEHICLE_TYPE}
      sortOrder={11}
      labelName="车型名称"
      valueName="车型编码"
      valueHelp="建议使用大写英文、数字和下划线，例如 REN_19"
    />
  );
}
