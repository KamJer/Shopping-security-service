package pl.kamjer.ShoppingSecService.config.security;

import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pl.kamjer.ShoppingSecService.repository.UserRepository;

@AllArgsConstructor
@Service
public class UserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return  userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("No such user found"))
                .convertToSpringUser();
    }
}
