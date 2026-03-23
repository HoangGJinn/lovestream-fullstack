package com.hcmute.lovestream.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation để kiểm tra trường password và confirmPassword có khớp nhau hay không.
 * Annotation này được đặt trực tiếp lên Class.
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordMatchingValidator.class)
public @interface PasswordMatching {

    String message() default "Mật khẩu xác nhận không khớp";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
