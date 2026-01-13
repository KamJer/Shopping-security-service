package pl.kamjer.ShoppingSecService.config.security;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import pl.kamjer.ShoppingSecService.config.components.SkipAuthorizationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Central configuration class for Spring Security in the application.
 * This class defines the security filters, authentication managers, password encoding,
 * and authorization rules for the application.
 * Annotations:
 * - @Configuration: Marks this class as a Spring configuration class.
 * - @EnableWebSecurity: Enables Spring Security's web security support.
 * - @AllArgsConstructor: Generates a constructor with all fields for dependency injection.
 * - @EnableGlobalMethodSecurity: Enables method-level security annotations (e.g., @PreAuthorize).
 */
@Configuration
@EnableWebSecurity
@AllArgsConstructor
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfiguration {
    /**
     * User details service for loading user information from the database.
     * This is used by Spring Security for authentication purposes.
     */
    private final UserDetailService userDetailsService;

    /**
     * Custom filter that allows skipping authorization for specific endpoints.
     * This filter is added before the default UsernamePasswordAuthenticationFilter.
     */
    private final SkipAuthorizationFilter skipAuthorizationFilter;

    /**
     * Configures the security filter chain for the application.
     * This method defines:
     * - Disables CSRF protection (common for stateless APIs).
     * - Registers a custom filter to skip authorization for specific endpoints.
     * - Defines authorization rules for HTTP requests.
     * - Enables HTTP Basic authentication.
     * - Configures headers (e.g., frame options).
     *
     * @param httpSecurity The HttpSecurity object used to configure security settings.
     * @return A configured SecurityFilterChain.
     * @throws Exception If an error occurs during configuration.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(skipAuthorizationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests((authz) ->
                        authz.anyRequest().permitAll())
                .httpBasic(withDefaults());

        httpSecurity.userDetailsService(userDetailsService);
        return httpSecurity.build();
    }

    /**
     * Configures and returns the AuthenticationManager bean.
     * This bean is responsible for managing the authentication process.
     *
     * @param userDetailsService The service used to load user details.
     * @param passwordEncoder    The encoder used to verify passwords.
     * @return A configured AuthenticationManager.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(authenticationProvider);
    }

    /**
     * Provides a BCryptPasswordEncoder bean for securely encoding passwords.
     * This is used by Spring Security to compare user-provided passwords with stored hashes.
     *
     * @return A new instance of BCryptPasswordEncoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}