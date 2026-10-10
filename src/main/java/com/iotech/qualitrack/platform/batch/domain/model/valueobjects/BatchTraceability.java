package com.iotech.qualitrack.platform.batch.domain.model.valueobjects;

import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;
import com.iotech.qualitrack.platform.batch.domain.model.aggregates.PharmaceuticalProduct;
import com.iotech.qualitrack.platform.batch.domain.model.entities.DigitalSignature;
import com.iotech.qualitrack.platform.batch.domain.model.entities.EquipmentUsage;
import com.iotech.qualitrack.platform.batch.domain.model.entities.RawMaterialUsage;
import com.iotech.qualitrack.platform.batch.domain.model.entities.RejectionRecord;
import com.iotech.qualitrack.platform.batch.domain.model.entities.StaffParticipation;

import java.util.List;
import java.util.Optional;

/**
 * Everything that took part in a product batch (US80, TS70): the raw material lots it consumed, the
 * equipment and staff that worked on it and the final release or rejection.
 *
 * @param batch the product batch
 * @param product the manufactured product
 * @param rawMaterials the consumed raw materials with the environment where each material is kept
 * @param equipment the equipment used
 * @param staff the staff who took part
 * @param release the release signature, when the batch was released
 * @param rejection the rejection record, when the batch was rejected
 * @param container the monitored container where the batch is stored, when it has one
 */
public record BatchTraceability(
        Batch batch,
        PharmaceuticalProduct product,
        List<TracedRawMaterialUsage> rawMaterials,
        List<EquipmentUsage> equipment,
        List<StaffParticipation> staff,
        Optional<DigitalSignature> release,
        Optional<RejectionRecord> rejection,
        Optional<BatchContainer> container
) {
    /**
     * A raw material usage together with the environment where the material is kept, if known.
     *
     * @param usage the usage
     * @param rawMaterialEnvironmentId the Inventory environment of the material; null for legacy usages
     */
    public record TracedRawMaterialUsage(RawMaterialUsage usage, Long rawMaterialEnvironmentId) {
    }
}
