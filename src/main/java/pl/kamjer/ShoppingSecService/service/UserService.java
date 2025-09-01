package pl.kamjer.ShoppingSecService.service;

import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pl.kamjer.ShoppingSecService.DatabaseUtil;
import pl.kamjer.ShoppingSecService.model.dto.UserDto;
import pl.kamjer.ShoppingSecService.exception.NoResourcesFoundException;
import pl.kamjer.ShoppingSecService.model.User;
import pl.kamjer.ShoppingSecService.repository.UserRepository;

import java.time.LocalDateTime;

@Service
public class UserService extends CustomService {

    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        super(userRepository);
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public LocalDateTime insertUser(UserDto userDto) {
        LocalDateTime savedTime = LocalDateTime.now();
        User user = DatabaseUtil.toUser(userDto);
        user.setSavedTime(savedTime);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return LocalDateTime.now();
    }

    @Transactional
    public void updateUser(UserDto userDto) {
        User userSec = getUserFromAuth();
        User user = DatabaseUtil.toUser(userDto);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User userToUpdate = userRepository.findByUserName(userSec.getUserName()).orElseThrow(() -> new NoResourcesFoundException("No such User found: " + userSec.getUserName()));
        userToUpdate.setUserName(user.getUserName());
        userToUpdate.setPassword(user.getPassword());
    }

    @Transactional
        public Boolean logUser(UserDto userDto) {
        return userRepository.findByUserName(userDto.getUserName())
                .map(user ->
                        passwordEncoder.matches(userDto.getPassword(), user.getPassword()))
                .orElse(false);
    }

    @Transactional
    public UserDto getUserByName(String userName) {
        return DatabaseUtil.toUserDto(userRepository.findByUserName(userName).orElseThrow(() -> new NoResourcesFoundException("No such User")));
    }
}
