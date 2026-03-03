package com.expmatik.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BarcodeValidator implements ConstraintValidator<ValidBarcode, String> {

    @Override
    public void initialize(ValidBarcode constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String barcode, ConstraintValidatorContext context) {
        // Permitir null para campos opcionales (se debe combinar con @NotBlank/@NotNull si es requerido)
        if (barcode == null) {
            return true;
        }
        
        // Si está presente, no debe estar vacío
        if (barcode.isBlank()) {
            return false;
        }

        // Verificar que solo contenga dígitos
        if (!barcode.matches("\\d+")) {
            return false;
        }

        // Verificar que la longitud sea 8 o 13
        int length = barcode.length();
        return length == 8 || length == 13;
    }
}
