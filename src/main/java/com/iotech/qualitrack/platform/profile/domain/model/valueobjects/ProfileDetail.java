package com.iotech.qualitrack.platform.profile.domain.model.valueobjects;

import com.iotech.qualitrack.platform.profile.domain.model.aggregates.Profile;

import java.util.List;

/**
 * Profile shown to a person: the personal data, with the account it belongs to and, for staff members, the staff
 * record of the laboratory.
 *
 * @param profile the profile; not stored yet when the person never saved it
 * @param username username of the account
 * @param email e-mail of the account, or null for old accounts without it
 * @param roles role names of the account
 * @param staffId staff record of the person in the laboratory, or null for the quality manager
 * @param position position registered by the quality manager, or null
 */
public record ProfileDetail(Profile profile, String username, String email, List<String> roles, Long staffId,
                            String position) {
}
