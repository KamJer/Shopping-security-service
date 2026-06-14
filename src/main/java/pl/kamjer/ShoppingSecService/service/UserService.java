package pl.kamjer.ShoppingSecService.service;

import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pl.kamjer.ShoppingSecService.exception.NoResourcesFoundException;
import pl.kamjer.ShoppingSecService.model.User;
import pl.kamjer.ShoppingSecService.model.dto.TokenDto;
import pl.kamjer.ShoppingSecService.model.dto.UserDto;
import pl.kamjer.ShoppingSecService.model.dto.UserInfoDto;
import pl.kamjer.ShoppingSecService.model.dto.UserRequestDto;
import pl.kamjer.ShoppingSecService.repository.UserRepository;

import java.time.LocalDateTime;

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

    @Transactional
    public UserDto getUserByName(String userName) {
        User user = userRepository.findByUserName(userName).orElseThrow(() -> new NoResourcesFoundException("No such User"));
        return UserDto.builder()
                .userName(user.getUserName())
                .savedTime(user.getSavedTime())
                .build();
    }

    public UserInfoDto validateUser(String token) {
        if(jwtService.isAccessValid(token)) {
            User user = userRepository.findByUserName(jwtService.extractUsernameAccess(token)).orElseThrow(() -> new BadCredentialsException("Invalid or expired token"));
            return UserInfoDto.builder()
                    .userName(user.getUserName())
                    .role(user.getRole())
                    .build();
        }
        throw new BadCredentialsException("Invalid or expired token");
    }
}
