package com.neatlogic.autoexecrunner.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 验证 Runner 覆盖 Jackson BOM 后与 Spring Boot 自动配置及 YAML 扩展的兼容性。 */
class JacksonCompatibilityTest {

    /** 使用实际自动配置验证日期、Optional 和 Unicode 文本的 JSON 往返转换。 */
    @Test
    void bootMapperPreservesDatesOptionalAndUnicode() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    ObjectMapper mapper = context.getBean(ObjectMapper.class);
                    LocalDateTime time = LocalDateTime.of(2026, 11, 5, 10, 30);
                    String json = mapper.writeValueAsString(Map.of(
                            "message", "执行成功 🚀",
                            "time", Optional.of(time)));
                    assertEquals("执行成功 🚀", mapper.readTree(json).get("message").asText());
                    assertEquals("2026-11-05T10:30:00", mapper.readTree(json).get("time").asText());
                    TypeReference<Optional<LocalDateTime>> type = new TypeReference<>() { };
                    assertEquals(Optional.of(time), mapper.readValue("\"2026-11-05T10:30:00\"", type));
                    assertFalse(mapper.readValue("null", type).isPresent());
                });
    }

    /** 验证 MCP 间接引入的 YAML 扩展可与统一版本的核心组件协同解析嵌套结构。 */
    @Test
    void yamlMapperPreservesNestedSchema() throws Exception {
        YAMLMapper mapper = new YAMLMapper();
        String yaml = "type: object\nproperties:\n  message:\n    type: string\n    description: 执行结果\n";
        Map<String, Object> schema = mapper.readValue(yaml, new TypeReference<>() { });
        assertEquals("object", schema.get("type"));
        assertEquals(Map.of("message", Map.of("type", "string", "description", "执行结果")),
                schema.get("properties"));
        assertEquals(schema, mapper.readValue(mapper.writeValueAsString(schema), new TypeReference<Map<String, Object>>() { }));
    }
}
