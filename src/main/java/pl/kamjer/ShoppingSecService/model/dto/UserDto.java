package pl.kamjer.ShoppingSecService.model.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;
import pl.kamjer.ShoppingSecService.validation.UniqUserNameConstraint;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserDto {

    @UniqUserNameConstraint
    @NotEmpty
    private String userName;
    private LocalDateTime savedTime;
}
