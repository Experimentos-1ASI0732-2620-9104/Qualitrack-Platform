package com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.services;

import com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.iotech.qualitrack.platform.shared.application.security.CurrentUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserImpl implements CurrentUser {
    @Override
    public Long userId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl user
                ? user.getId() : null;
    }

    @Override
    public boolean hasAnyAuthority(String... authorities) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;
        var wanted = java.util.Set.of(authorities);
        return authentication.getAuthorities().stream().anyMatch(granted -> wanted.contains(granted.getAuthority()));
    }
}
