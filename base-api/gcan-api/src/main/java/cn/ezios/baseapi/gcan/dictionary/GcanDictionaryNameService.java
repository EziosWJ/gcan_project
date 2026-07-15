package cn.ezios.baseapi.gcan.dictionary;

import java.util.List;
import java.util.Map;

public interface GcanDictionaryNameService {

    String name(String dictCode, String code);

    GcanDictionaryName resolve(String dictCode, String code);

    List<GcanDictionaryItem> items(String dictCode);

    Map<String, Map<String, String>> snapshot();

    boolean refreshNow();

    void refreshAsync();
}
