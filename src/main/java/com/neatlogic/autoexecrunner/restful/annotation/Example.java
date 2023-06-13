package com.neatlogic.autoexecrunner.restful.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Example {
	String example() default "";
}
