package com.neatlogic.autoexecrunner.init;

import com.neatlogic.autoexecrunner.common.RootConfiguration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

@Component
public class RootConfigurationLoader implements BeanDefinitionRegistryPostProcessor, PriorityOrdered {

    private static final String BASE_PACKAGE = "com.neatlogic"; // 修改成你项目的根包

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        // 创建扫描器（不使用默认过滤器，这样能扫到所有类）
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);

        // 添加只匹配 @RootConfiguration 的过滤器
        scanner.addIncludeFilter(new AnnotationTypeFilter(RootConfiguration.class));

        // 扫描包
        scanner.findCandidateComponents(BASE_PACKAGE).forEach(beanDefinition -> {
            String beanName = beanDefinition.getBeanClassName();
            // 注册 BeanDefinition
            registry.registerBeanDefinition(beanName, beanDefinition);
            System.out.println("📌 提前注册 RootConfiguration: " + beanName);
        });
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // 这里不需要处理
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // 确保最优先执行
    }
}

