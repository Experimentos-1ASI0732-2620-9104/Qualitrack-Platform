package com.iotech.qualitrack.platform.shared.application.security;

/** Identity of the authenticated actor; absent for system-originated events. */
public interface CurrentUser {
    Long userId();

    /**
     * Whether the authenticated actor holds any of the authorities, for example ROLE_QA_MANAGER.
     */
    boolean hasAnyAuthority(String... authorities);

    /**
     * Whether the actor is a quality manager or administrator, who can assign any staff member to an operation;
     * other staff members can only assign themselves.
     */
    default boolean managesQuality() {
        return hasAnyAuthority("ROLE_QA_MANAGER", "ROLE_ADMIN");
    }
}
