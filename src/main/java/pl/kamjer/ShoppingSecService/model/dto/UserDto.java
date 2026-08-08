package pl.kamjer.ShoppingSecService.model.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserDto {

    private LocalDateTime savedTime;
}
