package com.expmatik.backend.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BarcodeValidator.class)
public @interface ValidBarcode {
    String message() default "Barcode must be 8 or 13 digits";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
