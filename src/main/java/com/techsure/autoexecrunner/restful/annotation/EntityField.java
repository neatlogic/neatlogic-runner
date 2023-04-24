package com.techsure.autoexecrunner.restful.annotation;


import com.techsure.autoexecrunner.constvalue.ApiParamType;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EntityField {

    String name() default "";

    ApiParamType type() default ApiParamType.STRING;

    Class<?> member() default NotDefined.class;// 值成员

}
