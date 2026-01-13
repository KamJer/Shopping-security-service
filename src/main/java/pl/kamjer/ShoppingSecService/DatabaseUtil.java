package pl.kamjer.ShoppingSecService;

import lombok.extern.slf4j.Slf4j;
import pl.kamjer.ShoppingSecService.model.dto.UserDto;
import pl.kamjer.ShoppingSecService.model.User;

import java.time.LocalDateTime;

@Slf4j
public class DatabaseUtil {

    public static User toUser(UserDto userDto) {
        return User.builder()
                .userName(userDto.getUserName())
                .savedTime(LocalDateTime.now())
                .build();
    }
}
