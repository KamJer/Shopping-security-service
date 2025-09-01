package pl.kamjer.ShoppingSecService.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.userdetails.UserDetails;
import pl.kamjer.ShoppingSecService.validation.UniqUserNameConstraint;

import java.io.Serializable;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "\"USER\"")
public class User implements Serializable {

    @Id
    @Column(name = "user_name")
    @EqualsAndHashCode.Include
    private String userName;
    @Column(name = "password")
    private String password;
    @Version
    @Column(name = "saved_time")
    private LocalDateTime savedTime;

    public UserDetails convertToSpringUser() {
        return org.springframework.security.core.userdetails.User.builder()
                .username(this.getUserName())
                .password(this.getPassword())
                .roles("USER")
                .build();
    }
}
