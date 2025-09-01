package pl.kamjer.ShoppingSecService.controller.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import pl.kamjer.ShoppingSecService.exception.NoResourcesFoundException;

import java.security.Principal;
import java.util.Optional;

@ControllerAdvice
@Slf4j
public class ShoppingListControllerAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        StringBuilder errorStringBuilder = new StringBuilder();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errorStringBuilder
                    .append("field:")
                    .append(fieldName)
                    .append(" : ")
                    .append(errorMessage)
                    .append("\n");
        });
        log.error(errorStringBuilder.toString());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorStringBuilder.toString());
    }

    @ExceptionHandler({NoResourcesFoundException.class})
    public ResponseEntity<String> handleNotFoundExceptions(Exception ex, Principal principal) {
        String textForError = textForError(principal);
        log.error(textForError, ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalAccessError.class})
    public ResponseEntity<String> handleDeserializeException(Exception ex, Principal principal) {
        String textForError = textForError(principal);
        log.error(textForError, ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    private String textForError(Principal principal) {
        String text = "No user logged, public endpoint: ";
        Optional<Principal> optionalPrincipal = Optional.ofNullable(principal);
        if (optionalPrincipal.isPresent()) {
            text = "User logged: " + optionalPrincipal.get().getName() + ": ";
        }
        return text;
    }
}
