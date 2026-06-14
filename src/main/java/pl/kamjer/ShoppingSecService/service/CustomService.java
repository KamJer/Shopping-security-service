package pl.kamjer.ShoppingSecService.service;

import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import pl.kamjer.ShoppingSecService.exception.NoResourcesFoundException;
import pl.kamjer.ShoppingSecService.model.User;
import pl.kamjer.ShoppingSecService.repository.UserRepository;

@Service
@AllArgsConstructor
public class CustomService {

    protected UserRepository userRepository;

    public User getUserFromAuth() throws NoResourcesFoundException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new NoResourcesFoundException("No authentication found");
        }
        Object principal = authentication.getPrincipal();
        if (principal == null) {
            throw new NoResourcesFoundException("No authenticated user");
        }
        if (!(principal instanceof UserDetails userDetails)) {
            throw new NoResourcesFoundException("Unexpected principal type: " + principal.getClass());
        }
        return userRepository.findByUserName(userDetails.getUsername()).orElseThrow(() -> new NoResourcesFoundException("No such User"));
    }
}
