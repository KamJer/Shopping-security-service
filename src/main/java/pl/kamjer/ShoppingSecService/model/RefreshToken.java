package pl.kamjer.ShoppingSecService.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Setter
@Getter
public class RefreshToken {
    @Id
    private String jti;
    @ManyToOne
    @JoinColumn(name = "user_name")
    private User user;
    private LocalDateTime expirationTime;
    private boolean isRevoked;
}
