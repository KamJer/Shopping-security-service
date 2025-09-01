package pl.kamjer.ShoppingSecService.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.NOT_FOUND, reason="No Such Resources")
public class NoResourcesFoundException extends RuntimeException{
    public NoResourcesFoundException(String message) {
        super(message);
    }
}
