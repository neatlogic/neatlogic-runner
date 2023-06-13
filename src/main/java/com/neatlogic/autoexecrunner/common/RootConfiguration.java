package com.neatlogic.autoexecrunner.common;

import org.springframework.context.annotation.Configuration;

import java.lang.annotation.*;

/**
 * root-context定义配置管理类时通过此annotation作为过滤条件
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration
public @interface RootConfiguration {
}