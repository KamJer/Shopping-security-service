package pl.kamjer.ShoppingSecService.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.kamjer.ShoppingSecService.repository.UserRepository;

@Component
@NoArgsConstructor
public class UniqUserNameConstraintValidator implements ConstraintValidator<UniqUserNameConstraint, String> {

    private UserRepository userRepository;

    @Autowired
    public UniqUserNameConstraintValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean isValid(String userName, ConstraintValidatorContext constraintValidatorContext) {
        return !userRepository.existsByUserName(userName);
    }
}
