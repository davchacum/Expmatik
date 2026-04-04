package com.expmatik.backend.invoice;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
            assertThatThrownBy(() -> invokeParseBarcode("12A45678", 3))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid productBarcode");
        }
        @Test
        void invalidBarcodeWrongLength() {
            assertThatThrownBy(() -> invokeParseBarcode("1234567", 4))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid productBarcode");
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
            assertThatThrownBy(() -> invokeParsePositiveInteger("0", "quantity", 2))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be greater than 0");
            assertThatThrownBy(() -> invokeParsePositiveInteger("-1", "quantity", 3))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be greater than 0");
        }
        @Test
        void notANumber() {
            assertThatThrownBy(() -> invokeParsePositiveInteger("abc", "quantity", 4))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is not a valid integer");
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
            assertThatThrownBy(() -> invokeParsePositiveDecimal("0", "unitPrice", 2))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be greater than 0");
            assertThatThrownBy(() -> invokeParsePositiveDecimal("-1.5", "unitPrice", 3))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be greater than 0");
        }
        @Test
        void tooManyIntegerDigits() {
            assertThatThrownBy(() -> invokeParsePositiveDecimal("12345678901.00", "unitPrice", 4))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must have at most 10 integer digits");
        }
        @Test
        void tooManyFractionDigits() {
            assertThatThrownBy(() -> invokeParsePositiveDecimal("1.123", "unitPrice", 5))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must have at most 10 integer digits and 2 decimal places");
        }
        @Test
        void notADecimal() {
            assertThatThrownBy(() -> invokeParsePositiveDecimal("abc", "unitPrice", 6))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is not a valid decimal");
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
            assertThatThrownBy(() -> invokeParseOptionalDate("2026-13-10", "expirationDate", 3))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid expirationDate");
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
            assertThatThrownBy(() -> invokeParseDate("2026-02-30", "invoiceDate", 2))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid invoiceDate");
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
            assertThatThrownBy(() -> invokeParseStatus("unknown", 4))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid status");
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
}
