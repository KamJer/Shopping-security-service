package pl.kamjer.ShoppingSecService.model.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import pl.kamjer.ShoppingSecService.service.Role;
import pl.kamjer.ShoppingSecService.validation.UniqUserNameConstraint;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class UserRequestDto {
    @UniqUserNameConstraint
    @NotEmpty
    private String userName;
    private String password;
    private LocalDateTime savedTime;
    private Role role;
}
