package pl.kamjer.ShoppingSecService.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pl.kamjer.ShoppingSecService.model.dto.TokenDto;
import pl.kamjer.ShoppingSecService.model.dto.UserDto;
import pl.kamjer.ShoppingSecService.exception.NoResourcesFoundException;
import pl.kamjer.ShoppingSecService.model.User;
import pl.kamjer.ShoppingSecService.model.dto.UserInfoDto;
import pl.kamjer.ShoppingSecService.model.dto.UserRequestDto;
import pl.kamjer.ShoppingSecService.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService extends CustomService {

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final ObjectMapper objectMapper;

    private final JwtService jwtService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, ObjectMapper objectMapper, JwtService jwtService) {
        super(userRepository);
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.objectMapper = objectMapper;
        this.jwtService = jwtService;
    }

    @Transactional
    public LocalDateTime insertUser(UserRequestDto userDto) {
        LocalDateTime savedTime = LocalDateTime.now();
        User user = objectMapper.convertValue(userDto, User.class);
        user.setSavedTime(savedTime);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (userDto.getRole() == null) {
            user.setRole(Role.USER);
        }
        userRepository.save(user);
        return LocalDateTime.now();
    }

    @Transactional
    public void updateUser(UserRequestDto userDto) {
        User userSec = getUserFromAuth();
        User user = objectMapper.convertValue(userDto, User.class);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User userToUpdate = userRepository.findByUserName(userSec.getUserName()).orElseThrow(() -> new NoResourcesFoundException("No such User found: " + userSec.getUserName()));
        userToUpdate.setUserName(user.getUserName());
        userToUpdate.setPassword(user.getPassword());
        userToUpdate.setSavedTime(userDto.getSavedTime());
    }

    @Transactional
    public void updateUserSavedTime(UserDto userDto) {
        User userSec = getUserFromAuth();
        User userToUpdate = userRepository.findByUserName(userSec.getUserName()).orElseThrow(() -> new NoResourcesFoundException("No such User found: " + userSec.getUserName()));
        userToUpdate.setSavedTime(userDto.getSavedTime());
    }

    @Transactional
    public TokenDto logUser(UserRequestDto userDto) {
        Optional<User> optionalUser = userRepository.findByUserName(userDto.getUserName());
        if (optionalUser.isPresent()) {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            userDto.getUserName(),
                            userDto.getPassword()
                    )
            );
            return jwtService.newTokens(optionalUser.get());
        }
        throw new BadCredentialsException("");
    }

    @Transactional
    public UserDto getUserByName(String userName) {
        return objectMapper.convertValue(userRepository.findByUserName(userName).orElseThrow(() -> new NoResourcesFoundException("No such User")), UserDto.class);
    }

    public UserInfoDto validateUser(String token) {
        if(jwtService.isAccessValid(token)) {
            User user = userRepository.findByUserName(jwtService.extractUsernameAccess(token)).orElseThrow();
            return UserInfoDto.builder()
                    .userName(user.getUserName())
                    .role(user.getRole())
                    .build();
        }
        throw new BadCredentialsException("");
    }
}
