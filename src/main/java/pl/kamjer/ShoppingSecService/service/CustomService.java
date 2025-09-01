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
        String userName = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        return userRepository.findByUserName(userName).orElseThrow(() -> new NoResourcesFoundException("No such User"));
    }
}
