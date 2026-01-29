package com.bracepl.reporting_service.configs;


import com.bracepl.reporting_service.application.model.UserType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify which user types are allowed to access an endpoint
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAuth {
    UserType[] value() default {};
}