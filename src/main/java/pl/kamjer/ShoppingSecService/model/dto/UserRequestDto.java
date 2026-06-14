package pl.kamjer.ShoppingSecService.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import pl.kamjer.ShoppingSecService.validation.UniqUserNameConstraint;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class UserRequestDto {
    @UniqUserNameConstraint
    @NotEmpty
    private String userName;
    @NotEmpty(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    private String password;
    private LocalDateTime savedTime;
}
