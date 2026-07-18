package cn.ezios.baseapi.gcan.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "gcan")
public class GcanProperties {

    private Tcp tcp = new Tcp();
    private History history = new History();
    private Dictionary dictionary = new Dictionary();
    private Mirror mirror = new Mirror();
    private long frameStaleThresholdMs = 10000L;

    @Data
    public static class Tcp {
        private int port = 8000;
    }

    @Data
    public static class History {
        private boolean enabled = true;
        private long storeIntervalMs = 3000L;
        private List<String> includedCanIds = new ArrayList<>();
    }

    @Data
    public static class Dictionary {
        private String baseUrl = "http://localhost:8080";
        private String endpoint = "/api/open/gcan/v1/dictionaries";
        private long refreshIntervalMs = 60000L;
        private int connectTimeoutMs = 2000;
        private int readTimeoutMs = 3000;
    }

    @Data
    public static class Mirror {
        private boolean enabled;
        private String baseUrl = "";
        private String endpoint = "/api/open/gcan/v1/raw-frame/current";
        private long pollIntervalMs = 1000L;
        private int connectTimeoutMs = 2000;
        private int readTimeoutMs = 2000;
        private List<String> boxIds = new ArrayList<>();
        private List<String> canIds = new ArrayList<>();
    }
}
