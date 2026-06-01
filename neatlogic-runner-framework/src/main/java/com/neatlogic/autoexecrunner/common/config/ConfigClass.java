package com.neatlogic.autoexecrunner.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * @author lvzk
 * @since 2021/10/20 21:12
 **/
@Configuration
/**
 * ImportResource引入资源文件有三种方式：
 *     1.直接引入，该路径就是src/resources/下面的文件：file
 *     2.classpath引入：该路径就是src/java下面的配置文件：classpath:file
 *     3.引入本地文件：该路径是一种绝对路径：file:D://....
 */
@ImportResource(locations = {"classpath:init.xml"})
public class ConfigClass {
}
