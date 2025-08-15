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

    private static final String BASE_PACKAGE = "com.neatlogic";

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        // 这里只注册BeanDefinition，不创建实例
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RootConfiguration.class));

        scanner.findCandidateComponents(BASE_PACKAGE).forEach(beanDefinition -> {
            String beanClassName = beanDefinition.getBeanClassName();
            if (!registry.containsBeanDefinition(beanClassName)) {
                registry.registerBeanDefinition(beanClassName, beanDefinition);
                System.out.println("📌 提前注册 RootConfiguration: " + beanClassName);
            }
        });
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // 在BeanFactory阶段提前实例化这些Root配置类
        String[] rootConfigBeans = beanFactory.getBeanNamesForAnnotation(RootConfiguration.class);
        for (String beanName : rootConfigBeans) {
            System.out.println("🚀 提前实例化 RootConfiguration Bean: " + beanName);
            beanFactory.getBean(beanName); // 触发实例化 + @PostConstruct
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // 最优先执行
    }
}
