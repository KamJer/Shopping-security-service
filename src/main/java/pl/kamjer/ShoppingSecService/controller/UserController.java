package pl.kamjer.ShoppingSecService.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.kamjer.ShoppingSecService.model.User;
import pl.kamjer.ShoppingSecService.model.dto.UserDto;
import pl.kamjer.ShoppingSecService.model.dto.UserRequestDto;
import pl.kamjer.ShoppingSecService.service.UserService;

import java.time.LocalDateTime;

@RestController
@AllArgsConstructor
@RequestMapping(path = "/user")
@Slf4j
public class UserController {

    private UserService userService;

    @PutMapping(path = "/savedTime")
    public ResponseEntity<?> putUserSavedTime(@RequestBody UserDto user) {
        userService.updateUserSavedTime(user);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<LocalDateTime> postUser(@Valid @RequestBody UserRequestDto user) {
        return ResponseEntity.ok(userService.insertUser(user));
    }

    @PostMapping(path = "/log")
    public ResponseEntity<Boolean> logUser(@RequestBody UserRequestDto user) {
        return ResponseEntity.ok(userService.logUser(user));
    }

    @GetMapping (path = "/{userName}")
    public ResponseEntity<UserDto> getUserByName(@PathVariable String userName) {
        return ResponseEntity.ok(userService.getUserByName(userName));
    }
}
