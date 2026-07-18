package cn.ezios.baseapi.gcan.external;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ExternalMineNameStore {

    private final Map<String, String> names = new ConcurrentHashMap<>();

    public void put(String mineCode, String mineName) {
        if (StringUtils.hasText(mineCode) && StringUtils.hasText(mineName)) {
            names.put(mineCode.trim(), mineName.trim());
        }
    }

    public String name(String mineCode) {
        return names.getOrDefault(mineCode, mineCode);
    }
}
