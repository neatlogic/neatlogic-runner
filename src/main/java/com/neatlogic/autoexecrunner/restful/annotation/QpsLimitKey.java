package com.neatlogic.autoexecrunner.restful.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface QpsLimitKey {
    String value();
}
