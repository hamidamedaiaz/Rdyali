package com.softpath.riverpath.util;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ValidatedField {
    boolean nullable() default false;
    boolean isUnique() default false;
    boolean is3D() default false;
    boolean isDouble() default false;
}