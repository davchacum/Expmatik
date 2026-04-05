package com.expmatik.backend.invoice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.expmatik.backend.exceptions.BadRequestException;

public class InvoiceCSVLectorValidationTest {
    private final InvoiceCSVLector lector = new InvoiceCSVLector();

    @Nested
    @DisplayName("parseBarcode")
    class ParseBarcode {
        @Test
        void validBarcode8() {
            assertEquals("12345678", invokeParseBarcode("12345678", 1));
        }
        @Test
        void validBarcode13() {
            assertEquals("1234567890123", invokeParseBarcode("1234567890123", 2));
        }
        @Test
        void invalidBarcodeNonNumeric() {

            BadRequestException exception = assertThrows(BadRequestException.class, () -> invokeParseBarcode("12A45678", 3));

            assertEquals("Line 3: invalid productBarcode. It must be numeric and contain 8 or 13 digits.", exception.getMessage());
        }
        @Test
        void invalidBarcodeWrongLength() {
            BadRequestException exception = assertThrows(BadRequestException.class, () -> invokeParseBarcode("1234567", 4));
            assertEquals("Line 4: invalid productBarcode. It must be numeric and contain 8 or 13 digits.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("parsePositiveInteger")
    class ParsePositiveInteger {
        @Test
        void validInteger() {
            assertEquals(5, invokeParsePositiveInteger("5", "quantity", 1));
        }
        @Test
        void zeroOrNegative() {
            BadRequestException exception1 = assertThrows(BadRequestException.class, () -> invokeParsePositiveInteger("0", "quantity", 2));
            assertEquals("Line 2: quantity must be greater than 0.", exception1.getMessage());
            BadRequestException exception2 = assertThrows(BadRequestException.class, () -> invokeParsePositiveInteger("-1", "quantity", 3));
            assertEquals("Line 3: quantity must be greater than 0.", exception2.getMessage());
        }
        @Test
        void notANumber() {
            BadRequestException exception = assertThrows(BadRequestException.class, () -> invokeParsePositiveInteger("abc", "quantity", 4));
            assertEquals("Line 4: quantity is not a valid integer.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("parsePositiveDecimal")
    class ParsePositiveDecimal {
        @Test
        void validDecimal() {
            assertEquals(new BigDecimal("10.25"), invokeParsePositiveDecimal("10.25", "unitPrice", 1));
        }
        @Test
        void zeroOrNegative() {
            BadRequestException exception1 = assertThrows(BadRequestException.class, () -> invokeParsePositiveDecimal("0", "unitPrice", 2));
            assertEquals("Line 2: unitPrice must be greater than 0.", exception1.getMessage());
            BadRequestException exception2 = assertThrows(BadRequestException.class, () -> invokeParsePositiveDecimal("-1.5", "unitPrice", 3));
            assertEquals("Line 3: unitPrice must be greater than 0.", exception2.getMessage());
        }
        @Test
        void tooManyIntegerDigits() {
            BadRequestException exception = assertThrows(BadRequestException.class, () -> invokeParsePositiveDecimal("12345678901.00", "unitPrice", 4));
            assertEquals("Line 4: unitPrice must have at most 10 integer digits and 2 decimal places.", exception.getMessage());
        }
        @Test
        void tooManyFractionDigits() {
            BadRequestException exception = assertThrows(BadRequestException.class, () -> invokeParsePositiveDecimal("1.123", "unitPrice", 5));
            assertEquals("Line 5: unitPrice must have at most 10 integer digits and 2 decimal places.", exception.getMessage());
        }
        @Test
        void notADecimal() {
            BadRequestException exception = assertThrows(BadRequestException.class, () -> invokeParsePositiveDecimal("abc", "unitPrice", 6));
            assertEquals("Line 6: unitPrice is not a valid decimal.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("parseOptionalDate")
    class ParseOptionalDate {
        @Test
        void validDate() {
            assertEquals(LocalDate.of(2026, 3, 10), invokeParseOptionalDate("2026-03-10", "expirationDate", 1));
        }
        @Test
        void emptyReturnsNull() {
            assertNull(invokeParseOptionalDate("", "expirationDate", 2));
        }
        @Test
        void invalidDate() {
            BadRequestException exception = assertThrows(BadRequestException.class, () -> invokeParseOptionalDate("2026-13-10", "expirationDate", 3));
            assertEquals("Line 3: invalid expirationDate. Expected format: yyyy-MM-dd.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("parseDate")
    class ParseDate {
        @Test
        void validDate() {
            assertEquals(LocalDate.of(2026, 3, 10), invokeParseDate("2026-03-10", "invoiceDate", 1));
        }
        @Test
        void invalidDate() {
            BadRequestException exception = assertThrows(BadRequestException.class, () -> invokeParseDate("2026-02-30", "invoiceDate", 2));
            assertEquals("Line 2: invalid invoiceDate. Expected format: yyyy-MM-dd.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("parseStatus")
    class ParseStatus {
        @Test
        void validStatus() {
            assertEquals(InvoiceStatus.CANCELED, invokeParseStatus("CANCELED", 1));
            assertEquals(InvoiceStatus.PENDING, invokeParseStatus("PENDING", 2));
            assertEquals(InvoiceStatus.RECEIVED, invokeParseStatus("RECEIVED", 3));
        }
        @Test
        void invalidStatus() {
            BadRequestException exception = assertThrows(BadRequestException.class, () -> invokeParseStatus("unknown", 4));
            assertEquals("Line 4: invalid status -> UNKNOWN. Allowed values: PENDING, RECEIVED, CANCELED.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("requiredText")
    class RequiredText {
        @Test
        void validText() {
            assertEquals("Some text", requiredText("  Some text  ", "description", 1));
        }
        @Test
        void emptyOrBlank() {
            BadRequestException exception1 = assertThrows(BadRequestException.class, () -> requiredText("", "description", 2));
            assertEquals("Line 2: required field is empty -> description.", exception1.getMessage());
            BadRequestException exception2 = assertThrows(BadRequestException.class, () -> requiredText("   ", "description", 3));
            assertEquals("Line 3: required field is empty -> description.", exception2.getMessage());
            BadRequestException exception3 = assertThrows(BadRequestException.class, () -> requiredText(null, "description", 4));
            assertEquals("Line 4: required field is empty -> description.", exception3.getMessage());
        }
    }

    @Nested
    @DisplayName("isBlankRow")
    class IsBlankRow {
        @Test
        void blankRow() {
            assertTrue(invokeIsBlankRow(new String[]{"", " ", null}));
        }
        @Test
        void nonBlankRow() {
            assertFalse(invokeIsBlankRow(new String[]{"", "value", null}));
        }
    }

    // Reflection helpers to access private methods
    private String invokeParseBarcode(String value, int line) {
        try {
            var m = InvoiceCSVLector.class.getDeclaredMethod("parseBarcode", String.class, int.class);
            m.setAccessible(true);
            return (String) m.invoke(lector, value, line);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause != null) throw new RuntimeException(cause);
            throw new RuntimeException(e);
        }
    }
    private Integer invokeParsePositiveInteger(String value, String field, int line) {
        try {
            var m = InvoiceCSVLector.class.getDeclaredMethod("parsePositiveInteger", String.class, String.class, int.class);
            m.setAccessible(true);
            return (Integer) m.invoke(lector, value, field, line);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause != null) throw new RuntimeException(cause);
            throw new RuntimeException(e);
        }
    }

    private InvoiceStatus invokeParseStatus(String value, int line) {
        try {
            var m = InvoiceCSVLector.class.getDeclaredMethod("parseStatus", String.class, int.class);
            m.setAccessible(true);
            return (InvoiceStatus) m.invoke(lector, value, line);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause != null) throw new RuntimeException(cause);
            throw new RuntimeException(e);
        }
    }

    private BigDecimal invokeParsePositiveDecimal(String value, String field, int line) {
        try {
            var m = InvoiceCSVLector.class.getDeclaredMethod("parsePositiveDecimal", String.class, String.class, int.class);
            m.setAccessible(true);
            return (BigDecimal) m.invoke(lector, value, field, line);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause != null) throw new RuntimeException(cause);
            throw new RuntimeException(e);
        }
    }
    private LocalDate invokeParseOptionalDate(String value, String field, int line) {
        try {
            var m = InvoiceCSVLector.class.getDeclaredMethod("parseOptionalDate", String.class, String.class, int.class);
            m.setAccessible(true);
            return (LocalDate) m.invoke(lector, value, field, line);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause != null) throw new RuntimeException(cause);
            throw new RuntimeException(e);
        }
    }
    private LocalDate invokeParseDate(String value, String field, int line) {
        try {
            var m = InvoiceCSVLector.class.getDeclaredMethod("parseDate", String.class, String.class, int.class);
            m.setAccessible(true);
            return (LocalDate) m.invoke(lector, value, field, line);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause != null) throw new RuntimeException(cause);
            throw new RuntimeException(e);
        }
    }
    private boolean invokeIsBlankRow(String[] row) {
        try {
            var m = InvoiceCSVLector.class.getDeclaredMethod("isBlankRow", String[].class);
            m.setAccessible(true);
            return (boolean) m.invoke(lector, (Object) row);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause != null) throw new RuntimeException(cause);
            throw new RuntimeException(e);
        }
    }
    private String requiredText(String value, String fieldName, int line) {
        try {
            var m = InvoiceCSVLector.class.getDeclaredMethod("requiredText", String.class, String.class, int.class);
            m.setAccessible(true);
            return (String) m.invoke(lector, value, fieldName, line);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause != null) throw new RuntimeException(cause);
            throw new RuntimeException(e);
        }
    }
}
