package dev.brian.kata.config;

import dev.brian.kata.error.JsonSecurityErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JsonSecurityErrorHandler jsonSecurityErrorHandler)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/ping", "/api/greeting", "/api/echo").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/books", "/api/books/**").permitAll()
                        .requestMatchers("/api/books", "/api/books/**").hasRole("LIBRARIAN")
                        .requestMatchers("/api/members", "/api/members/**").hasRole("LIBRARIAN")
                        .requestMatchers("/api/loans", "/api/loans/**").authenticated()
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint(jsonSecurityErrorHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jsonSecurityErrorHandler)
                        .accessDeniedHandler(jsonSecurityErrorHandler));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("librarian")
                        .password(passwordEncoder.encode("librarian-pass"))
                        .roles("LIBRARIAN")
                        .build(),
                User.withUsername("member")
                        .password(passwordEncoder.encode("member-pass"))
                        .roles("MEMBER")
                        .build());
    }
}
