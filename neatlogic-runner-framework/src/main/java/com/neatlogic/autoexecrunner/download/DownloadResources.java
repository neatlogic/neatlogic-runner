package com.neatlogic.autoexecrunner.download;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class DownloadResources implements WebMvcConfigurer {
    @Value("${tagent.download.path:/app/autoexec/data/tagent/}")
    private String location; //上传文件保存的本地目录

    @Value("${autoexec.download.path:/app/autoexec/data/informant/}")
    private String informantLocation; //上传文件保存的本地目录
    /**
     * 配置静态资源映射
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //匹配到resourceHandler,将URL映射至location,也就是本地文件夹
        registry.addResourceHandler("/tagent/download/**").addResourceLocations("file:"+location);

        registry.addResourceHandler("/informant/download/**").addResourceLocations("file:"+informantLocation);
    }
}
