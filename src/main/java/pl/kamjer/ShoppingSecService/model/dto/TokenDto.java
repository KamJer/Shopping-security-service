package pl.kamjer.ShoppingSecService.model.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TokenDto {

    private String refreshToken;
    private String accessToken;
}
