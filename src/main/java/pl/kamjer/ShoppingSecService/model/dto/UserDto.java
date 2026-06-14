package pl.kamjer.ShoppingSecService.model.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserDto {

    @NotEmpty
    private String userName;
    private LocalDateTime savedTime;
}
