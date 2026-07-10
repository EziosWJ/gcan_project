import { DICT_CODES } from "@/constants/dicts";
import { FixedDictPage } from "./fixed-dict-page";

export function GcanMinesPage() {
  return (
    <FixedDictPage
      title="煤矿维护"
      description="维护车辆档案使用的煤矿字典项。"
      dictCode={DICT_CODES.GCAN_MINE}
      sortOrder={10}
      labelName="煤矿名称"
      valueName="煤矿 ID"
    />
  );
}
