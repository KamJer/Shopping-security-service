package pl.kamjer.ShoppingSecService.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.kamjer.ShoppingSecService.model.User;
import pl.kamjer.ShoppingSecService.repository.UserRepository;
import pl.kamjer.ShoppingSecService.service.Role;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username:}")
    private String adminUserName;

    @Value("${admin.password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminUserName == null || adminUserName.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            log.info("No admin bootstrap credentials provided (admin.username/admin.password), skipping");
            return;
        }
        User admin = userRepository.findByUserName(adminUserName)
                .map(user -> {
                    user.setRole(Role.SUPER_ADMIN);
                    user.setPassword(passwordEncoder.encode(adminPassword));
                    return user;
                })
                .orElseGet(() -> User.builder()
                        .userName(adminUserName)
                        .password(passwordEncoder.encode(adminPassword))
                        .savedTime(LocalDateTime.now())
                        .role(Role.SUPER_ADMIN)
                        .build());
        userRepository.save(admin);
        log.info("Super admin user '{}' ensured with role SUPER_ADMIN (credentials from environment)", adminUserName);
    }
}
