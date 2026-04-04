package com.expmatik.backend.sale;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.expmatik.backend.exceptions.BadRequestException;

public class SaleCSVLectorValidationTest {

    private final SaleCSVLector lector = new SaleCSVLector();

    @Nested
    @DisplayName("parseDecimal")
    class ParseDecimal {
        @Test
        void validDecimal() {
            assertEquals(new BigDecimal("10.25"), invokeParseDecimal("10.25", "totalAmount", 1));
        }
        @Test
        void notADecimal() {
            assertThatThrownBy(() -> invokeParseDecimal("abc", "totalAmount", 2))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is not a valid decimal");
        }
    }

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

    @Nested
    @DisplayName("parseDateTime")
    class ParseDateTime {
        @Test
        void validDateTime() {
            assertEquals(LocalDateTime.of(2026, 3, 10, 12, 0), invokeParseDateTime("2026-03-10T12:00:00", 1));
        }
        @Test
        void invalidDateTime() {
            assertThatThrownBy(() -> invokeParseDateTime("2026-02-30T12:00:00", 2))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid saleDate");
        }
    }

    @Nested
    @DisplayName("parseTransactionStatus")
    class ParseTransactionStatus {
        @Test
        void validStatus() {
            assertEquals(TransactionStatus.SUCCESS, invokeParseTransactionStatus("SUCCESS", 1));
            assertEquals(TransactionStatus.FAILED, invokeParseTransactionStatus("FAILED", 2));
        }
        @Test
        void invalidStatus() {
            assertThatThrownBy(() -> invokeParseTransactionStatus("INVALID", 2))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid status");
        }
    }

    @Nested
    @DisplayName("parsePaymentMethod")
    class ParsePaymentMethod {
        @Test
        void validPaymentMethods() {
            assertEquals(PaymentMethod.CASH, invokeParsePaymentMethod("CASH", 1));
            assertEquals(PaymentMethod.CREDIT_CARD, invokeParsePaymentMethod("CREDIT_CARD", 2));
        }
        @Test
        void invalidPaymentMethod() {
            assertThatThrownBy(() -> invokeParsePaymentMethod("INVALID", 3))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid payment method");
        }
    }

    @Nested
    @DisplayName("parsePositiveInteger")
    class ParsePositiveInteger {
        @Test
        void validInteger() {
            assertEquals(5, invokeParsePositiveInteger("5", "rowNumber", 1));
        }
        @Test
        void zeroOrNegative() {
            assertThatThrownBy(() -> invokeParsePositiveInteger("0", "rowNumber", 2))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be greater than 0");
            assertThatThrownBy(() -> invokeParsePositiveInteger("-1", "rowNumber", 3))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be greater than 0");
        }
        @Test
        void notANumber() {
            assertThatThrownBy(() -> invokeParsePositiveInteger("abc", "rowNumber", 4))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is not a valid integer");
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
            assertThatThrownBy(() -> requiredText("", "description", 2))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("required field is empty");
            assertThatThrownBy(() -> requiredText("   ", "description", 3))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("required field is empty");
            assertThatThrownBy(() -> requiredText(null, "description", 4))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("required field is empty");
        }
    }

    private BigDecimal invokeParseDecimal(String value, String field, int line) {
        try {
            var m = SaleCSVLector.class.getDeclaredMethod("parseDecimal", String.class, String.class, int.class);
            m.setAccessible(true);
            return (BigDecimal) m.invoke(lector, value, field, line);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause != null) throw new RuntimeException(cause);
            throw new RuntimeException(e);
        }
    }

    private LocalDateTime invokeParseDateTime(String value, int line) {
        try {
            var m = SaleCSVLector.class.getDeclaredMethod("parseDateTime", String.class, int.class);
            m.setAccessible(true);
            return (LocalDateTime) m.invoke(lector, value, line);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause != null) throw new RuntimeException(cause);
            throw new RuntimeException(e);
        }
    }

    private boolean invokeIsBlankRow(String[] row) {
        try {
            var m = SaleCSVLector.class.getDeclaredMethod("isBlankRow", String[].class);
            m.setAccessible(true);
            return (boolean) m.invoke(lector, (Object) row);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause != null) throw new RuntimeException(cause);
            throw new RuntimeException(e);
        }
    }
    private String invokeParseBarcode(String value, int line) {
        try {
            var m = SaleCSVLector.class.getDeclaredMethod("parseBarcode", String.class, int.class);
            m.setAccessible(true);
            return (String) m.invoke(lector, value, line);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause != null) throw new RuntimeException(cause);
            throw new RuntimeException(e);
        }
    }

    private TransactionStatus invokeParseTransactionStatus(String value, int line) {
        try {
            var m = SaleCSVLector.class.getDeclaredMethod("parseStatus", String.class, int.class);
            m.setAccessible(true);
            return (TransactionStatus) m.invoke(lector, value, line);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause != null) throw new RuntimeException(cause);
            throw new RuntimeException(e);
        }
    }

    private PaymentMethod invokeParsePaymentMethod(String value, int line) {
        try {
            var m = SaleCSVLector.class.getDeclaredMethod("parsePaymentMethod", String.class, int.class);
            m.setAccessible(true);
            return (PaymentMethod) m.invoke(lector, value, line);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause != null) throw new RuntimeException(cause);
            throw new RuntimeException(e);
        }
    }

    private Integer invokeParsePositiveInteger(String value, String field, int line) {
        try {
            var m = SaleCSVLector.class.getDeclaredMethod("parsePositiveInteger", String.class, String.class, int.class);
            m.setAccessible(true);
            return (Integer) m.invoke(lector, value, field, line);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause != null) throw new RuntimeException(cause);
            throw new RuntimeException(e);
        }
    }
        private String requiredText(String value, String fieldName, int line) {
        try {
            var m = SaleCSVLector.class.getDeclaredMethod("requiredText", String.class, String.class, int.class);
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
