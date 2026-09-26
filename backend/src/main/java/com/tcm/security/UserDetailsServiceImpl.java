package com.tcm.security;

import com.tcm.user.UserRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(UserPrincipal::new)
                // Deliberately generic: DaoAuthenticationProvider converts this into a
                // BadCredentialsException (hideUserNotFoundExceptions defaults to true),
                // so a wrong password and an unknown email look identical to the caller.
                .orElseThrow(() -> new UsernameNotFoundException("No user with email " + email));
    }

    /**
     * The JWT filter's lookup: a token names its user by id ({@code sub}),
     * which - unlike the email - never changes. Empty for a user who no
     * longer exists.
     */
    public Optional<UserPrincipal> findById(UUID id) {
        return userRepository.findById(id).map(UserPrincipal::new);
    }
}
