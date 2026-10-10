package com.iotech.qualitrack.platform.iam.domain.repositories;

import com.iotech.qualitrack.platform.iam.domain.model.aggregates.User;

import java.util.List;
import java.util.Optional;

/**
 * User repository port.
 *
 * <p>Defines persistence operations for IAM user aggregate roots.</p>
 */
public interface UserRepository {

    Optional<User> findById(Long id);

    Optional<User> findByIdForUpdate(Long id);

    Optional<User> findByUsername(String username);

    List<User> findAll();

    /**
     * @param laboratoryId laboratory of the accounts
     * @return accounts of the laboratory, active or not
     */
    List<User> findByLaboratoryId(Long laboratoryId);

    User save(User user);

    boolean existsById(Long id);

    boolean existsByUsername(String username);

    /**
     * @param email normalized e-mail (lower case)
     */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
