package com.expmatik.backend.invoice;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.expmatik.backend.batch.DTOs.BatchCreate;
import com.expmatik.backend.exceptions.BadRequestException;
import com.expmatik.backend.invoice.DTOs.InvoiceRequest;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

@Component
public class InvoiceCSVLector {

    private static final int EXPECTED_COLUMNS = 8;

    public List<InvoiceRequest> readCSV(File file) {

        Map<String, InvoiceRequest> invoices = new LinkedHashMap<>();

        try (CSVReader reader = new CSVReader(new FileReader(file))) {

            String[] row;
            int line = 0;

            while ((row = reader.readNext()) != null) {

                line++;

                if (isBlankRow(row) || (line == 1 && isHeader(row))) {
                    continue;
                }

                ParsedRow parsed = parseRow(row, line);
                accumulateInvoice(invoices, parsed, line);
            }
        } catch (IOException | CsvValidationException e) {
            throw new BadRequestException("Error reading CSV: " + e.getMessage());
        }

        if (invoices.isEmpty()) {
            throw new BadRequestException("The CSV does not contain valid invoice records.");
        }

        return invoices.values().stream().toList();
    }

    private ParsedRow parseRow(String[] row, int line) {

        validateColumnCount(row, line);

        String invoiceNumber = requiredText(row[0], "invoiceNumber", line);
        String supplierName = requiredText(row[1], "supplierName", line);

        InvoiceStatus status = parseStatus(row[2], line);
        LocalDate invoiceDate = parseDate(requiredText(row[3], "invoiceDate", line), "invoiceDate", line);

        BatchCreate batch = new BatchCreate(
                parseOptionalDate(row[7], "expirationDate", line),
                parsePositiveDecimal(row[6], "unitPrice", line),
                parsePositiveInteger(row[5], "quantity", line),
                parseBarcode(row[4], line));

        return new ParsedRow(invoiceNumber, supplierName, status, invoiceDate, batch);
    }

    private void accumulateInvoice(
            Map<String, InvoiceRequest> invoices,
            ParsedRow row,
            int line) {

        InvoiceRequest invoice = invoices.get(row.invoiceNumber());

        if (invoice == null) {

            List<BatchCreate> batches = new ArrayList<>();
            batches.add(row.batch());

            invoices.put(
                    row.invoiceNumber(),
                    new InvoiceRequest(
                            row.invoiceNumber(),
                            row.supplierName(),
                            row.status(),
                            batches,
                            row.invoiceDate()));

        } else {

            ensureSameHeader(invoice, row.supplierName(), row.status(), row.invoiceDate(), line);
            invoice.batches().add(row.batch());
        }
    }

    private void validateColumnCount(String[] row, int line) {

        if (row.length != EXPECTED_COLUMNS) {
            throw new BadRequestException(
                    "Line " + line + ": expected " + EXPECTED_COLUMNS + " columns but found " + row.length + ".");
        }
    }

    private void ensureSameHeader(
            InvoiceRequest invoice,
            String supplierName,
            InvoiceStatus status,
            LocalDate invoiceDate,
            int line) {

        if (!invoice.supplierName().equals(supplierName)) {
            throw new BadRequestException(
                    "Line " + line + ": inconsistent supplierName for invoice " + invoice.invoiceNumber() + ".");
        }

        if (invoice.status() != status) {
            throw new BadRequestException(
                    "Line " + line + ": inconsistent status for invoice " + invoice.invoiceNumber() + ".");
        }

        if (!invoice.invoiceDate().equals(invoiceDate)) {
            throw new BadRequestException(
                    "Line " + line + ": inconsistent invoiceDate for invoice " + invoice.invoiceNumber() + ".");
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

    private InvoiceStatus parseStatus(String value, int line) {

        String normalized = requiredText(value, "status", line).toUpperCase(Locale.ROOT);

        try {
            return InvoiceStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                    "Line " + line + ": invalid status -> " + normalized +
                            ". Allowed values: PENDING, RECEIVED, CANCELED.");
        }
    }

    private String parseBarcode(String value, int line) {

        String barcode = requiredText(value, "productBarcode", line);

        if (!barcode.matches("\\d+") || (barcode.length() != 8 && barcode.length() != 13)) {
            throw new BadRequestException(
                    "Line " + line + ": invalid productBarcode. It must be numeric and contain 8 or 13 digits.");
        }

        return barcode;
    }

    private Integer parsePositiveInteger(String value, String fieldName, int line) {

        String normalized = requiredText(value, fieldName, line);

        try {

            int number = Integer.parseInt(normalized);

            if (number <= 0) {
                throw new BadRequestException(
                        "Line " + line + ": " + fieldName + " must be greater than 0.");
            }

            return number;

        } catch (NumberFormatException ex) {

            throw new BadRequestException(
                    "Line " + line + ": " + fieldName + " is not a valid integer.");
        }
    }

    private BigDecimal parsePositiveDecimal(String value, String fieldName, int line) {

        String normalized = requiredText(value, fieldName, line);

        try {

            BigDecimal number = new BigDecimal(normalized);
            int integerDigits = number.precision() - number.scale();
            int fractionDigits = Math.max(number.scale(), 0);

            if (number.signum() <= 0) {
                throw new BadRequestException(
                        "Line " + line + ": " + fieldName + " must be greater than 0.");
            }

            if (integerDigits > 10 || fractionDigits > 2) {
                throw new BadRequestException(
                        "Line " + line + ": " + fieldName + " must have at most 10 integer digits and 2 decimal places.");
            }

            return number;

        } catch (NumberFormatException ex) {

            throw new BadRequestException(
                    "Line " + line + ": " + fieldName + " is not a valid decimal.");
        }
    }

    private LocalDate parseOptionalDate(String value, String fieldName, int line) {

        String normalized = normalize(value);

        if (normalized.isEmpty()) {
            return null;
        }

        return parseDate(normalized, fieldName, line);
    }

    private LocalDate parseDate(String value, String fieldName, int line) {

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException(
                    "Line " + line + ": invalid " + fieldName + ". Expected format: yyyy-MM-dd.");
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

        return row.length > 0 && "invoiceNumber".equalsIgnoreCase(normalize(row[0]));
    }

    private String normalize(String value) {

        return value == null ? "" : value.trim();
    }

    private record ParsedRow(
            String invoiceNumber,
            String supplierName,
            InvoiceStatus status,
            LocalDate invoiceDate,
            BatchCreate batch) {
    }
}
