package pl.kamjer.ShoppingSecService.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pl.kamjer.ShoppingSecService.model.dto.TokenDto;
import pl.kamjer.ShoppingSecService.model.dto.UserDto;
import pl.kamjer.ShoppingSecService.model.dto.UserInfoDto;
import pl.kamjer.ShoppingSecService.model.dto.UserRequestDto;
import pl.kamjer.ShoppingSecService.service.JwtService;
import pl.kamjer.ShoppingSecService.service.UserService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static pl.kamjer.ShoppingSecService.service.JwtService.REFRESH_TOKEN_EXP_DAYS;

@RestController
@AllArgsConstructor
@RequestMapping(path = "/user")
@Slf4j
public class UserController {

    private UserService userService;
    private JwtService jwtService;

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
    public ResponseEntity<TokenDto> loginUser(@RequestBody UserRequestDto user) {
        TokenDto tokens = userService.logUser(user);
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokens.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(REFRESH_TOKEN_EXP_DAYS))
                .sameSite("Strict")
                .build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie.toString()).body(userService.logUser(user));
    }

    @GetMapping(path = "/logout")
    public ResponseEntity<Boolean> logoutUser() {
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie.toString()).body(true);
    }

    @GetMapping(path = "/refresh")
    public ResponseEntity<TokenDto> refresh(HttpServletRequest request) {
        String token = request.getHeader("Authorization") == null ?
                Stream.of(request.getCookies())
                        .filter(cookie -> cookie.getName().equals("refreshToken"))
                        .map(Cookie::getValue)
                        .findFirst()
                        .orElseThrow(() -> new BadCredentialsException("No token found"))
                : request.getHeader("Authorization");
        TokenDto tokens = jwtService.refresh(token);
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokens.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(REFRESH_TOKEN_EXP_DAYS))
                .sameSite("Strict")
                .build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie.toString()).body(tokens);
    }

    @GetMapping
    public ResponseEntity<UserInfoDto> validateUser(@RequestParam String token) {
        return ResponseEntity.ok(userService.validateUser(token));
    }

    @GetMapping(path = "/{userName}")
    public ResponseEntity<UserDto> getUserByName(@PathVariable String userName) {
        return ResponseEntity.ok(userService.getUserByName(userName));
    }
}
