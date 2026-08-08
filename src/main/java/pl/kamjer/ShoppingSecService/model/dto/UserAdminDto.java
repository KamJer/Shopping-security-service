package pl.kamjer.ShoppingSecService.model.dto;

import lombok.Builder;
import lombok.Getter;
import pl.kamjer.ShoppingSecService.service.Role;

import java.time.LocalDateTime;

@Builder
@Getter
public class UserAdminDto {
    private String userName;
    private Role role;
    private LocalDateTime savedTime;
}
