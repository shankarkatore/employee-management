package com.empmgmt.security;

import com.empmgmt.security.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/do-login", "/error","/register-admin","/forgot-password","/reset-password", "/css/**","/js/**").permitAll()
                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/do-login")
                //.defaultSuccessUrl("/web/dashboard", true)
                .successHandler((request, response, authentication) -> {

                    boolean isAdmin = authentication.getAuthorities()
                            .stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                    boolean isHr = authentication.getAuthorities()
                            .stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_HR"));

                    boolean isEmployee = authentication.getAuthorities()
                            .stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));

                    if (isAdmin || isHr) {
                        response.sendRedirect("/web/dashboard");
                    } else if (isEmployee) {
                        response.sendRedirect("/web/dashboard");
                    }
                })
                .failureUrl("/login?error=true")
                .permitAll()
            )

            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )

            .authenticationProvider(authenticationProvider());

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
