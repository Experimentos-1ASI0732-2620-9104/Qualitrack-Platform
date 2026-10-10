package com.iotech.qualitrack.platform.batch.domain.model.entities;

import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * The DigitalSignature domain entity.
 *
 * <p>Represents the digital approval evidence generated when a production batch
 * is released. It supports traceability and regulatory compliance.</p>
 */
@Getter
public class DigitalSignature {

    /**
     * The unique internal numeric identifier for the signature.
     */
    private Long id;

    /**
     * The numeric identifier of the released batch.
     */
    private Long batchId;

    /**
     * The numeric identifier of the user who signed the release.
     */
    private Long signedByUserId;

    /**
     * The generated signature hash.
     */
    private String signatureHash;

    /**
     * The date and time when the release was signed.
     */
    private String signedAt;

    /**
     * Default constructor.
     * Required by the persistence and mapping layers to reconstruct the entity.
     */
    public DigitalSignature() {
        // Required for reconstruction by JPA or Assemblers
    }

    /**
     * Reconstructs a DigitalSignature entity from persistence data.
     *
     * @param id The unique numeric ID.
     * @param batchId The batch ID.
     * @param signedByUserId The signing user ID.
     * @param signatureHash The generated signature hash.
     * @param signedAt The signing date.
     */
    public DigitalSignature(Long id, Long batchId, Long signedByUserId, String signatureHash, String signedAt) {
        this.id = id;
        this.batchId = batchId;
        this.signedByUserId = signedByUserId;
        this.signatureHash = signatureHash;
        this.signedAt = signedAt;
    }

    /**
     * Constructs a new DigitalSignature.
     *
     * @param batchId The batch ID. Cannot be null or less than 1.
     * @param signedByUserId The signing user ID. Cannot be null or less than 1.
     * @param signatureHash The generated signature hash. Cannot be null or blank.
     * @param signedAt The signing date. Cannot be null or blank.
     */
    public DigitalSignature(Long batchId, Long signedByUserId, String signatureHash, String signedAt) {
        if (batchId == null || batchId <= 0) {
            throw new IllegalArgumentException("batchId cannot be null or less than 1");
        }
        if (signedByUserId == null || signedByUserId <= 0) {
            throw new IllegalArgumentException("signedByUserId cannot be null or less than 1");
        }
        if (signatureHash == null || signatureHash.isBlank()) {
            throw new IllegalArgumentException("signatureHash cannot be null or blank");
        }
        if (signedAt == null || signedAt.isBlank()) {
            throw new IllegalArgumentException("signedAt cannot be null or blank");
        }

        this.batchId = batchId;
        this.signedByUserId = signedByUserId;
        this.signatureHash = signatureHash;
        this.signedAt = signedAt;
    }

    /**
     * Signs the release of a batch on behalf of the user who confirmed it (US81).
     *
     * <p>The hash is the SHA-256 of the batch identity, its release date and the signer, so any later
     * change of those values no longer matches the stored evidence.</p>
     *
     * @param batch the released batch
     * @param signedByUserId the user who confirmed the release
     * @param signedAt the moment of the signature
     * @return the signature evidence to store
     */
    public static DigitalSignature sign(Batch batch, Long signedByUserId, Instant signedAt) {
        var content = String.join("|", String.valueOf(batch.getId()), batch.getBatchNumber(), String.valueOf(batch.getLabId()),
                String.valueOf(batch.getProductId()), batch.getEndDate(), String.valueOf(signedByUserId), signedAt.toString());
        try {
            var hash = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            return new DigitalSignature(batch.getId(), signedByUserId, HexFormat.of().formatHex(hash), signedAt.toString());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
