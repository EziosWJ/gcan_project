package cn.ezios.baseapi.gcan;

import cn.ezios.baseapi.gcan.config.GcanProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan(basePackages = {
        "cn.ezios.baseapi.gcan.**.mapper",
        "cn.ezios.baseapi.gcan.config"
})
@SpringBootApplication
@EnableConfigurationProperties(GcanProperties.class)
public class GcanApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(GcanApiApplication.class, args);
    }
}
