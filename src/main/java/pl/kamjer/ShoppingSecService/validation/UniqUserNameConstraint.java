package pl.kamjer.ShoppingSecService.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UniqUserNameConstraintValidator.class)
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqUserNameConstraint {

    String message() default "User name is taken, try with different name";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
