package pl.kamjer.ShoppingSecService.config.security;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import pl.kamjer.ShoppingSecService.service.JwtService;

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
@EnableMethodSecurity
public class WebSecurityConfiguration {

    private AuthEntryPoint unauthorizedHandler;

    private JwtService jwtService;

    private JwtAuthFilter jwtAuthFilter;

    /**
     * User details service for loading user information from the database.
     * This is used by Spring Security for authentication purposes.
     */
    private final UserDetailService userDetailService;

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
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests((authz) ->
                        authz
                                .requestMatchers(HttpMethod.GET, "/user").permitAll()
                                .requestMatchers(HttpMethod.GET, "/user/logout").permitAll()
                                .requestMatchers("/user/log").permitAll()
                                .requestMatchers(HttpMethod.POST, "/user").permitAll()
                                .anyRequest().authenticated()

                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
