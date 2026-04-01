package pl.kamjer.ShoppingSecService.model.dto;

import lombok.Builder;
import lombok.Getter;
import pl.kamjer.ShoppingSecService.service.Role;

@Builder
@Getter
public class UserInfoDto {
    private String userName;
    private Role role;
}
