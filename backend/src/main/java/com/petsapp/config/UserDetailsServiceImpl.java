package com.petsapp.config;

import com.petsapp.auth.User;
import com.petsapp.auth.UserRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacja UserDetailsService wymagana przez Spring Security.
 *
 * <p>
 * Laduje uzytkownika po emailu (email jest naszym primary login identifier).
 * Uzywana przez
 * DaoAuthenticationProvider przy logowaniu przez formularz — w naszym przypadku
 * JWT eliminuje
 * potrzebe sesji, ale komponent jest wymagany przez Spring Security do
 * konfiguracji PasswordEncoder.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository
                .findActiveByEmail(email)
                .orElseThrow(
                        () -> new UsernameNotFoundException("User not found with email: " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash() != null ? user.getPasswordHash() : "",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
