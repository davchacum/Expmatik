package com.expmatik.backend.vendingSlot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SlotLabelFormatterTest {

    @Test
    @DisplayName("toFrontendLabel should format one-based row and column as A1 style")
    void testToFrontendLabel_ShouldFormatSimpleCase() {
        assertEquals("A1", SlotLabelFormatter.toFrontendLabel(1, 1));
        assertEquals("B3", SlotLabelFormatter.toFrontendLabel(3, 2));
    }

    @Test
    @DisplayName("toFrontendLabel should support columns beyond Z")
    void testToFrontendLabel_ShouldFormatMultiLetterColumns() {
        assertEquals("AA10", SlotLabelFormatter.toFrontendLabel(10, 27));
        assertEquals("AZ7", SlotLabelFormatter.toFrontendLabel(7, 52));
    }

    @Test
    @DisplayName("toFrontendLabel should fallback to coordinate tuple for invalid values")
    void testToFrontendLabel_ShouldFallbackForInvalidValues() {
        assertEquals("(null,1)", SlotLabelFormatter.toFrontendLabel(null, 1));
        assertEquals("(1,null)", SlotLabelFormatter.toFrontendLabel(1, null));
        assertEquals("(0,1)", SlotLabelFormatter.toFrontendLabel(0, 1));
        assertEquals("(1,0)", SlotLabelFormatter.toFrontendLabel(1, 0));
    }
}
