package cn.ezios.baseapi.modules.system.dict.contract;

import java.util.List;
import lombok.Data;

@Data
public class GcanDictionaryVO {

    private String dictCode;

    private List<GcanDictionaryItemVO> items;
}
