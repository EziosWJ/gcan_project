package cn.ezios.baseapi.gcan.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class GcanPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class)
            .withPropertyValues(
                    "gcan.mirror.enabled=true",
                    "gcan.mirror.base-url=http://47.96.10.182:8081",
                    "gcan.mirror.box-ids=31,33",
                    "gcan.mirror.can-ids=08F200A0,1836FF30");

    @Test
    void bindsMirrorListsFromCommaSeparatedEnvironmentStyleValues() {
        contextRunner.run(context -> {
            GcanProperties properties = context.getBean(GcanProperties.class);

            assertEquals("http://47.96.10.182:8081", properties.getMirror().getBaseUrl());
            assertEquals(List.of("31", "33"), properties.getMirror().getBoxIds());
            assertEquals(List.of("08F200A0", "1836FF30"), properties.getMirror().getCanIds());
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(GcanProperties.class)
    static class PropertiesConfiguration {
    }
}
