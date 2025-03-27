package com.example.personal_finance_tracker.app.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Encode {
    String algorithm() default "BASE64";
    boolean decrypt() default true;
    boolean sensitive() default false; // Flag for extra sensitive fields
}