package com.expmatik.backend.sale;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.expmatik.backend.exceptions.BadRequestException;
import com.expmatik.backend.sale.DTOs.SaleCreate;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

@Component
public class SaleCSVLector {

    private static final int EXPECTED_COLUMNS = 7;

    public List<SaleCreate> readCSV(File file) {

        List<SaleCreate> sales = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(file))) {

            String[] row;
            int line = 0;

            while ((row = reader.readNext()) != null) {

                line++;

                if (isBlankRow(row) || (line == 1 && isHeader(row))) {
                    continue;
                }

                sales.add(parseRow(row, line));
            }

        } catch (IOException | CsvValidationException e) {
            throw new BadRequestException("Error reading CSV: " + e.getMessage());
        }

        if (sales.isEmpty()) {
            throw new BadRequestException("The CSV does not contain valid sales.");
        }

        return sales;
    }

    private SaleCreate parseRow(String[] row, int line) {

        validateColumnCount(row, line);

        LocalDateTime saleDate = parseDateTime(requiredText(row[0], "saleDate", line), line);
        BigDecimal totalAmount = parseDecimal(row[1], "totalAmount", line);
        PaymentMethod paymentMethod = parsePaymentMethod(row[2], line);
        TransactionStatus status = parseStatus(row[3], line);
        String barcode = requiredText(row[4], "barcode", line);
        UUID vendingSlotId = parseUUID(row[5], "vendingSlotId", line);

        return new SaleCreate(
                saleDate,
                totalAmount,
                paymentMethod,
                status,
                barcode,
                vendingSlotId
        );
    }

    private void validateColumnCount(String[] row, int line) {

        if (row.length != EXPECTED_COLUMNS) {
            throw new BadRequestException(
                    "Line " + line + ": expected " + EXPECTED_COLUMNS +
                            " columns but found " + row.length + ".");
        }
    }

    private String requiredText(String value, String fieldName, int line) {

        String normalized = normalize(value);

        if (normalized.isEmpty()) {
            throw new BadRequestException(
                    "Line " + line + ": required field is empty -> " + fieldName + ".");
        }

        return normalized;
    }

    private PaymentMethod parsePaymentMethod(String value, int line) {

        String normalized = requiredText(value, "paymentMethod", line).toUpperCase(Locale.ROOT);

        try {
            return PaymentMethod.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                    "Line " + line + ": invalid paymentMethod -> " + normalized + ".");
        }
    }

    private TransactionStatus parseStatus(String value, int line) {

        String normalized = requiredText(value, "status", line).toUpperCase(Locale.ROOT);

        try {
            return TransactionStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                    "Line " + line + ": invalid status -> " + normalized + ".");
        }
    }

    private BigDecimal parseDecimal(String value, String fieldName, int line) {

        String normalized = requiredText(value, fieldName, line);

        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            throw new BadRequestException(
                    "Line " + line + ": " + fieldName + " is not a valid decimal.");
        }
    }

    private UUID parseUUID(String value, String fieldName, int line) {

        try {
            return UUID.fromString(requiredText(value, fieldName, line));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                    "Line " + line + ": invalid UUID -> " + fieldName);
        }
    }

    private LocalDateTime parseDateTime(String value, int line) {

        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException(
                    "Line " + line +
                            ": invalid saleDate. Expected format: yyyy-MM-ddTHH:mm:ss");
        }
    }

    private boolean isBlankRow(String[] row) {

        for (String cell : row) {
            if (!normalize(cell).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private boolean isHeader(String[] row) {

        return row.length > 0 && "saleDate".equalsIgnoreCase(normalize(row[0]));
    }

    private String normalize(String value) {

        return value == null ? "" : value.trim();
    }

    public byte[] generateCSV(List<Sale> sales) {

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out);
            CSVWriter csvWriter = new CSVWriter(writer)) {

            csvWriter.writeNext(new String[]{
                    "saleId",
                    "saleDate",
                    "totalAmount",
                    "paymentMethod",
                    "status",
                    "barcode",
                    "vendingSlotId",
                    "failureReason"
            });

            for (Sale sale : sales) {

                csvWriter.writeNext(new String[]{
                        sale.getId() != null ? sale.getId().toString() : "",
                        formatDateTime(sale.getSaleDate()),
                        sale.getTotalAmount().toPlainString(),
                        sale.getPaymentMethod().name(),
                        sale.getStatus().name(),
                        sale.getProduct().getBarcode(),
                        sale.getVendingSlot().getId().toString(),
                        sale.getFailureReason() != null ? sale.getFailureReason() : ""
                });
            }
            csvWriter.flush();
            return out.toByteArray();

        } catch (IOException e) {
            throw new BadRequestException("Error generating CSV: " + e.getMessage());
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.toString();
    }
}