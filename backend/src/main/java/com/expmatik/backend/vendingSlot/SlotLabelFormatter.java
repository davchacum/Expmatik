package com.expmatik.backend.vendingSlot;

public final class SlotLabelFormatter {

    private SlotLabelFormatter() {
    }

    public static String toFrontendLabel(Integer rowNumber, Integer columnNumber) {
        if (rowNumber == null || columnNumber == null || rowNumber < 1 || columnNumber < 1) {
            return "(" + rowNumber + "," + columnNumber + ")";
        }

        return toColumnLabel(columnNumber) + rowNumber;
    }

    private static String toColumnLabel(int oneBasedColumnNumber) {
        StringBuilder label = new StringBuilder();
        int current = oneBasedColumnNumber;

        while (current > 0) {
            current--;
            label.append((char) ('A' + (current % 26)));
            current = current / 26;
        }

        return label.reverse().toString();
    }
}
