package com.iotech.qualitrack.platform.laboratory.domain.model.valueobjects;

/**
 * Main use assigned to an environment of a laboratory or pharmaceutical warehouse.
 *
 * <p>Distinguishes laboratory, production and storage zones so other bounded contexts
 * can place raw materials, products and devices in the right physical area.</p>
 */
public enum EnvironmentUsage {
    LABORATORY,
    PRODUCTION,
    RAW_MATERIAL_STORAGE,
    PRODUCT_STORAGE,
    OTHER
}
