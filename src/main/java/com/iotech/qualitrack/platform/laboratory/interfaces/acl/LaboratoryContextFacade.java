package com.iotech.qualitrack.platform.laboratory.interfaces.acl;

import java.util.List;
import java.util.Optional;

/**
 * Inbound Anti-Corruption Layer (ACL) facade for the Laboratory bounded context.
 * * <p>Exposes simplified capabilities to other bounded contexts (like Equipment or Tracking)
 * without exposing internal domain models or requiring them to know about internal queries/commands.</p>
 */
public interface LaboratoryContextFacade {

    /**
     * Verifies if a laboratory exists by its ID.
     *
     * @param labId the numeric identity of the laboratory
     * @return true if it exists, false otherwise
     */
    boolean existsLaboratoryById(Long labId);

    /**
     * Verifies if an environment exists and belongs to the given laboratory.
     *
     * @param laboratoryId the laboratory that must own the environment
     * @param environmentId the environment identifier
     * @return true when the environment exists in the laboratory
     */
    boolean existsEnvironment(Long laboratoryId, Long environmentId);

    /**
     * Finds an environment of the laboratory with its usage.
     *
     * @param laboratoryId the laboratory that must own the environment
     * @param environmentId the environment identifier
     * @return the environment, or empty when it does not exist in the laboratory
     */
    Optional<EnvironmentReference> findEnvironment(Long laboratoryId, Long environmentId);

    /**
     * Environments of the laboratory with their usage, ordered by code (reports and indicators, US93-US97).
     *
     * @param laboratoryId the laboratory
     * @return the environments; empty when the laboratory has none
     */
    List<EnvironmentReference> findEnvironments(Long laboratoryId);

    /**
     * Environment data shared with other bounded contexts.
     *
     * @param id the environment identifier
     * @param laboratoryId the owning laboratory
     * @param code the environment code, unique in the laboratory
     * @param name the environment name
     * @param usage the usage name (LABORATORY, PRODUCTION, RAW_MATERIAL_STORAGE, PRODUCT_STORAGE or OTHER), or null
     *              when no usage is assigned
     */
    record EnvironmentReference(Long id, Long laboratoryId, String code, String name, String usage) {
        /**
         * @param expected the usage name to compare
         * @return true when the environment has that usage
         */
        public boolean hasUsage(String expected) {
            return expected != null && expected.equals(usage);
        }
    }

    /**
     * Finds a staff member registered in the laboratory.
     *
     * @param laboratoryId the laboratory that must own the staff member
     * @param staffId the staff member identifier
     * @return the staff member, or empty when it is not registered in the laboratory
     */
    Optional<StaffReference> findStaffMember(Long laboratoryId, Long staffId);

    /**
     * @param userId the sign-in account
     * @return the staff member that signs in with the account, if it is one
     */
    Optional<StaffReference> findStaffMemberByAccount(Long userId);

    /**
     * Staff data shared with other bounded contexts.
     *
     * @param id the staff member identifier
     * @param laboratoryId the owning laboratory
     * @param fullName the full name
     * @param role the role in the laboratory
     * @param active whether the staff member is active
     * @param userId the account with which the staff member signs in, or null for older records
     * @param accessRole OPERATOR or AUDITOR, or null for older records
     */
    record StaffReference(Long id, Long laboratoryId, String fullName, String role, boolean active, Long userId,
                          String accessRole) {
        /**
         * Whether the staff member is the authenticated user.
         */
        public boolean isAccount(Long userId) {
            return this.userId != null && this.userId.equals(userId);
        }

        /**
         * Whether the staff member is an auditor, who only consults the records and cannot take part in operations.
         */
        public boolean isAuditor() {
            return "AUDITOR".equals(accessRole);
        }
    }
}