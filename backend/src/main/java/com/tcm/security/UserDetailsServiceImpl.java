package com.tcm.security;

import com.tcm.user.UserRepository;
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
}
