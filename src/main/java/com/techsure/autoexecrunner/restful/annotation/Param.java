package com.techsure.autoexecrunner.restful.annotation;

import com.techsure.autoexecrunner.constvalue.ApiParamType;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Param {

    String name() default "";

    ApiParamType type() default ApiParamType.STRING;

    boolean isRequired() default false;

    String rule() default "";

    String desc() default "";


    Class<?> explode() default NotDefined.class;

    boolean xss() default false;

    int maxLength() default -1;

    int minLength() default -1;
}
