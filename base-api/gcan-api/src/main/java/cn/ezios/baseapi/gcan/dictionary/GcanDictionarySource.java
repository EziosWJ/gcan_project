package cn.ezios.baseapi.gcan.dictionary;

import java.util.List;

public interface GcanDictionarySource {

    List<GcanDictionaryGroup> load();
}
