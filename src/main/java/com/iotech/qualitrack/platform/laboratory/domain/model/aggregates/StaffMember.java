package com.iotech.qualitrack.platform.laboratory.domain.model.aggregates;

import com.iotech.qualitrack.platform.laboratory.domain.model.commands.RegisterStaffCommand;
import com.iotech.qualitrack.platform.laboratory.domain.model.valueobjects.StaffAccessRole;
import com.iotech.qualitrack.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import lombok.Getter;

import java.util.Objects;

/**
 * The StaffMember Aggregate Root.
 *
 * <p>Represents an employee or personnel operating within a specific laboratory.
 * This entity governs the lifecycle of a staff member's assignment, their role,
 * the account they use to sign in and their operational status (active/inactive).</p>
 */
@Getter
public class StaffMember extends AbstractDomainAggregateRoot<StaffMember> {

    /**
     * The unique numeric identifier for the staff member.
     */
    private Long id;

    /**
     * The numeric identifier of the laboratory this staff member belongs to.
     */
    private Long laboratoryId;

    /**
     * The full legal name of the staff member.
     */
    private String fullName;

    /**
     * The assigned role or professional title.
     */
    private String role;

    /**
     * Institutional or corporate email address.
     */
    private String email;

    /**
     * Indicates whether the staff member is currently active in the system.
     */
    private boolean active;

    /**
     * What the staff member can do with their account; null for staff registered before accounts existed.
     */
    private StaffAccessRole accessRole;

    /**
     * Account (IAM user) with which the staff member signs in; null for staff registered before accounts existed.
     */
    private Long userId;

    /**
     * Default constructor.
     * <p>Required by the persistence and mapping layers (Assemblers) to reconstruct
     * the entity from the database without triggering business logic.</p>
     */
    public StaffMember() {
        // Required for reconstruction by JPA or Assemblers
    }

    /**
     * Reconstructs a StaffMember entity from persistence data.
     *
     * @param id The unique numeric staff member ID.
     * @param laboratoryId The numeric ID of the associated laboratory.
     * @param fullName The staff member's full name.
     * @param role The assigned role or job title.
     * @param email The corporate email address.
     * @param active The current operational status.
     * @param accessRole What the staff member can do (can be null for older records).
     * @param userId The account of the staff member (can be null for older records).
     */
    public StaffMember(Long id, Long laboratoryId, String fullName, String role, String email, boolean active,
                       StaffAccessRole accessRole, Long userId) {
        this.id = id;
        this.laboratoryId = laboratoryId;
        this.fullName = fullName;
        this.role = role;
        this.email = email;
        this.active = active;
        this.accessRole = accessRole;
        this.userId = userId;
    }

    /**
     * Registers a new StaffMember based on the provided command.
     * <p>Initializes the entity and sets the default status to active. The account is linked
     * once it is created in Identity and Access Management.</p>
     *
     * @param command The command containing the registration data.
     */
    public StaffMember(RegisterStaffCommand command) {
        this.laboratoryId = Objects.requireNonNull(command.laboratoryId(), "Laboratory ID is required");
        this.fullName = Objects.requireNonNull(command.fullName(), "Full name is required");
        this.role = Objects.requireNonNull(command.role(), "Role is required");
        this.email = Objects.requireNonNull(command.email(), "Email is required");
        this.accessRole = Objects.requireNonNull(command.accessRole(), "Access role is required");
        this.active = true;
    }

    /**
     * Links the account the staff member uses to sign in.
     *
     * @param userId the IAM user created for the staff member
     */
    public void linkAccount(Long userId) {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("A positive user identifier is required");
        if (this.userId != null && !this.userId.equals(userId)) {
            throw new IllegalStateException("The staff member already has an account");
        }
        this.userId = userId;
    }

    /**
     * Keeps the name in line with the one the staff member keeps in the profile.
     *
     * @param fullName name of the staff member
     * @return true when it changed
     */
    public boolean rename(String fullName) {
        if (fullName == null || fullName.isBlank()) return false;
        var name = fullName.trim();
        if (name.equals(this.fullName)) return false;
        this.fullName = name;
        return true;
    }

    /**
     * Keeps the e-mail in line with the one of the sign-in account.
     *
     * @param email e-mail of the account
     * @return true when it changed
     */
    public boolean changeEmail(String email) {
        if (email == null || email.isBlank() || email.equals(this.email)) return false;
        this.email = email;
        return true;
    }

    public boolean belongsTo(Long laboratoryId) {
        return Objects.equals(this.laboratoryId, laboratoryId);
    }

    /**
     * Deactivates the staff member's access or assignment.
     * <p>Business Rule: An already inactive staff member cannot be deactivated again.</p>
     *
     * @throws IllegalStateException if the staff member is already inactive.
     */
    public void deactivate() {
        if (!this.active) {
            throw new IllegalStateException("Staff member is already inactive.");
        }
        this.active = false;
    }
}
