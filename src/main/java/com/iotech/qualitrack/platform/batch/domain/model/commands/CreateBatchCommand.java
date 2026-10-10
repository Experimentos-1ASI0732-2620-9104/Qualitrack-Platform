package com.iotech.qualitrack.platform.batch.domain.model.commands;

import com.iotech.qualitrack.platform.shared.domain.model.valueobjects.StockUnit;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Command to register a manufacturing batch of a product (US73, TS63).
 *
 * @param laboratoryId the laboratory identifier
 * @param environmentId the environment where the product is manufactured
 * @param productId the product to manufacture
 * @param batchNumber the traceability code, unique in the laboratory (maximum 50 characters)
 * @param quantity the intended amount to produce, greater than zero
 * @param unit the production unit (defaults to units)
 * @param startDate the start date in ISO 8601 format (yyyy-MM-dd)
 * @param notes optional manufacturing notes (maximum 500 characters)
 */
public record CreateBatchCommand(
        Long laboratoryId,
        Long environmentId,
        Long productId,
        String batchNumber,
        Double quantity,
        String unit,
        String startDate,
        String notes
) {
    public CreateBatchCommand {
        if (laboratoryId == null || laboratoryId <= 0) throw new IllegalArgumentException("laboratoryId cannot be null or less than 1");
        if (environmentId == null || environmentId <= 0) throw new IllegalArgumentException("environmentId cannot be null or less than 1");
        if (productId == null || productId <= 0) throw new IllegalArgumentException("productId cannot be null or less than 1");
        if (batchNumber == null || batchNumber.isBlank()) throw new IllegalArgumentException("batchNumber cannot be null or blank");
        batchNumber = batchNumber.trim();
        if (batchNumber.length() > 50) throw new IllegalArgumentException("batchNumber cannot exceed 50 characters");
        if (quantity == null || !Double.isFinite(quantity) || quantity <= 0) {
            throw new IllegalArgumentException("quantity cannot be null or less than or equal to 0");
        }
        unit = StockUnit.normalize(unit == null || unit.isBlank() ? "units" : unit);
        startDate = isoDate(startDate, "startDate");
        notes = notes == null || notes.isBlank() ? null : notes.trim();
        if (notes != null && notes.length() > 500) throw new IllegalArgumentException("notes cannot exceed 500 characters");
    }

    static String isoDate(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " cannot be null or blank");
        try {
            return LocalDate.parse(value.trim()).toString();
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(field + " must be an ISO date (yyyy-MM-dd)");
        }
    }
}
