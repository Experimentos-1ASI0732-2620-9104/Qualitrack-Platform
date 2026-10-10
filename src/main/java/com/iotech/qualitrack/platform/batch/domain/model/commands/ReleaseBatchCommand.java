package com.iotech.qualitrack.platform.batch.domain.model.commands;

/**
 * Command to release a product batch after quality review (US81, TS71).
 *
 * @param laboratoryId the laboratory identifier
 * @param environmentId the environment identifier
 * @param productId the product identifier
 * @param batchId the batch to release
 * @param releaseDate the release date in ISO 8601 format (yyyy-MM-dd)
 * @param notes final quality remarks (maximum 500 characters)
 */
public record ReleaseBatchCommand(
        Long laboratoryId,
        Long environmentId,
        Long productId,
        Long batchId,
        String releaseDate,
        String notes
) {
    public ReleaseBatchCommand {
        if (batchId == null || batchId <= 0) throw new IllegalArgumentException("batchId cannot be null or less than 1");
        releaseDate = CreateBatchCommand.isoDate(releaseDate, "releaseDate");
        if (notes == null || notes.isBlank()) throw new IllegalArgumentException("notes cannot be null or blank");
        notes = notes.trim();
        if (notes.length() > 500) throw new IllegalArgumentException("notes cannot exceed 500 characters");
    }
}
