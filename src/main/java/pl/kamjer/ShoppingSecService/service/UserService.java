package pl.kamjer.ShoppingSecService.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pl.kamjer.ShoppingSecService.DatabaseUtil;
import pl.kamjer.ShoppingSecService.model.dto.UserDto;
import pl.kamjer.ShoppingSecService.exception.NoResourcesFoundException;
import pl.kamjer.ShoppingSecService.model.User;
import pl.kamjer.ShoppingSecService.model.dto.UserRequestDto;
import pl.kamjer.ShoppingSecService.repository.UserRepository;

import java.time.LocalDateTime;

@Service
public class UserService extends CustomService {

    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, ObjectMapper objectMapper) {
        super(userRepository);
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public LocalDateTime insertUser(UserRequestDto userDto) {
        LocalDateTime savedTime = LocalDateTime.now();
        User user = objectMapper.convertValue(userDto, User.class);
        user.setSavedTime(savedTime);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
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
        public Boolean logUser(UserRequestDto userDto) {
        return userRepository.findByUserName(userDto.getUserName())
                .map(user ->
                        passwordEncoder.matches(userDto.getPassword(), user.getPassword()))
                .orElse(false);
    }

    @Transactional
    public UserDto getUserByName(String userName) {
        return objectMapper.convertValue(userRepository.findByUserName(userName).orElseThrow(() -> new NoResourcesFoundException("No such User")), UserDto.class);
    }
}
