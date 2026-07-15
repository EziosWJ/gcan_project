package cn.ezios.baseapi.gcan.dictionary;

import java.util.List;
import lombok.Data;

@Data
public class GcanDictionaryGroup {

    private String dictCode;

    private List<GcanDictionaryItem> items;
}
