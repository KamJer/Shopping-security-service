package pl.kamjer.ShoppingSecService.service;

import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pl.kamjer.ShoppingSecService.exception.ForbiddenException;
import pl.kamjer.ShoppingSecService.exception.NoResourcesFoundException;
import pl.kamjer.ShoppingSecService.model.User;
import pl.kamjer.ShoppingSecService.model.dto.TokenDto;
import pl.kamjer.ShoppingSecService.model.dto.UserAdminDto;
import pl.kamjer.ShoppingSecService.model.dto.UserDto;
import pl.kamjer.ShoppingSecService.model.dto.UserInfoDto;
import pl.kamjer.ShoppingSecService.model.dto.UserRequestDto;
import pl.kamjer.ShoppingSecService.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService extends CustomService {

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService) {
        super(userRepository);
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public LocalDateTime insertUser(UserRequestDto userDto) {
        LocalDateTime savedTime = LocalDateTime.now();
        User user = User.builder()
                .userName(userDto.getUserName())
                .password(passwordEncoder.encode(userDto.getPassword()))
                .savedTime(savedTime)
                .role(Role.USER)
                .build();
        userRepository.save(user);
        return savedTime;
    }

    @Transactional
    public void updateUserSavedTime(UserDto userDto) {
        User userSec = getUserFromAuth();
        User userToUpdate = userRepository.findByUserName(userSec.getUserName()).orElseThrow(() -> new NoResourcesFoundException("No such User found: " + userSec.getUserName()));
        userToUpdate.setSavedTime(userDto.getSavedTime());
    }

    @Transactional
    public TokenDto logUser(UserRequestDto userDto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userDto.getUserName(),
                        userDto.getPassword()
                )
        );
        User user = userRepository.findByUserName(userDto.getUserName())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        return jwtService.newTokens(user);
    }

    public UserInfoDto validateUser(String token) {
        if (jwtService.isAccessValid(token)) {
            User user = userRepository.findByUserName(jwtService.extractUsernameAccess(token)).orElseThrow(() -> new BadCredentialsException("Invalid or expired token"));
            return UserInfoDto.builder()
                    .userName(user.getUserName())
                    .savedTime(user.getSavedTime())
                    .role(user.getRole())
                    .build();
        }
        throw new BadCredentialsException("Invalid or expired token");
    }

    @Transactional
    public List<UserAdminDto> getAllUsers() {
        return userRepository.findAllByOrderByUserNameAsc().stream()
                .map(user -> UserAdminDto.builder()
                        .userName(user.getUserName())
                        .role(user.getRole())
                        .savedTime(user.getSavedTime())
                        .build())
                .toList();
    }

    @Transactional
    public void changeUserRole(String userName, Role role) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new NoResourcesFoundException("No such User found: " + userName));
        if (getUserFromAuth().getUserName().equals(userName)) {
            throw new ForbiddenException("You cannot change your own role: " + userName);
        }
        if (user.getRole() == Role.SUPER_ADMIN) {
            throw new ForbiddenException("The SUPER_ADMIN account cannot be demoted: " + userName);
        }
        if (role == Role.SUPER_ADMIN) {
            throw new ForbiddenException("The SUPER_ADMIN role can only be granted via bootstrap configuration");
        }
        if (role == Role.USER && user.getRole() == Role.ADMIN
                && userRepository.countByRole(Role.ADMIN) <= 1
                && userRepository.countByRole(Role.SUPER_ADMIN) == 0) {
            throw new ForbiddenException("Cannot demote the last ADMIN user: " + userName);
        }
        user.setRole(role);
    }

    @Transactional
    public void deleteUser(String userName) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new NoResourcesFoundException("No such User found: " + userName));
        if (getUserFromAuth().getUserName().equals(userName)) {
            throw new ForbiddenException("You cannot delete your own account: " + userName);
        }
        if (user.getRole() == Role.SUPER_ADMIN) {
            throw new ForbiddenException("The SUPER_ADMIN account cannot be deleted: " + userName);
        }
        if (user.getRole() == Role.ADMIN
                && userRepository.countByRole(Role.ADMIN) <= 1
                && userRepository.countByRole(Role.SUPER_ADMIN) == 0) {
            throw new ForbiddenException("Cannot delete the last ADMIN user: " + userName);
        }
        userRepository.delete(user);
    }

    @Transactional
    public void changeUserPassword(String userName, String newPassword) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new NoResourcesFoundException("No such User found: " + userName));
        if (user.getRole() == Role.SUPER_ADMIN) {
            throw new ForbiddenException("The SUPER_ADMIN account cannot be modified: " + userName);
        }
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be between 8 and 64 characters");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        jwtService.revokeAllTokensForUser(userName);
    }
}
