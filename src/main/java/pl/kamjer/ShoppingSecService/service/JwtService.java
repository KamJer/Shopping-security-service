package pl.kamjer.ShoppingSecService.service;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.kamjer.ShoppingSecService.model.RefreshToken;
import pl.kamjer.ShoppingSecService.model.User;
import pl.kamjer.ShoppingSecService.model.dto.TokenDto;
import pl.kamjer.ShoppingSecService.repository.JwtRepository;

import javax.crypto.SecretKey;
import javax.swing.text.html.Option;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtService {
    public final static int ACCESS_TOKEN_EXP_MIN = 15;
    public final static int REFRESH_TOKEN_EXP_DAYS = 14;

    private final JwtRepository jwtRepository;

    private final SecretKey keyAccess;
    private final SecretKey keyRefresh;

    public JwtService(JwtRepository jwtRepository,
                      @Value("${jwt.secret_key.access}") String jwtSecretAccess,
                      @Value("${jwt.secret_key.refresh}") String jwtSecretRefresh) {
        this.jwtRepository = jwtRepository;

        keyAccess = Keys.hmacShaKeyFor(jwtSecretAccess.getBytes(StandardCharsets.UTF_8));
        keyRefresh = Keys.hmacShaKeyFor(jwtSecretRefresh.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String username) {
        LocalDateTime expiration = LocalDateTime.now().plusMinutes(ACCESS_TOKEN_EXP_MIN);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(expiration.atZone(ZoneId.systemDefault()).toInstant())) // 5min
                .signWith(keyAccess)
                .compact();
    }

    private JwtParser getParserAccess() {
        return Jwts.parserBuilder()
                .setSigningKey(keyAccess)
                .build();
    }

    private JwtParser getParserRefresh() {
        return Jwts.parserBuilder()
                .setSigningKey(keyRefresh)
                .build();
    }

    public String extractUsernameAccess(String token) {
        return getParserAccess()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String extractUserNameRefresh(String token) {
        return getParserRefresh()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String extractJti(String token) {
        try {
            return getParserRefresh().
                    parseClaimsJws(token)
                    .getBody()
                    .getId();
        } catch (MalformedJwtException ex) {
            throw new BadCredentialsException("Malformed token");
        }
    }

    public boolean isAccessValid(String token) {
        try {
            getParserAccess().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshValid(String token) {
        try {
            getParserRefresh().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String generateRefreshToken(User user) {
        String jti = UUID.randomUUID().toString();

        LocalDateTime expiration = LocalDateTime.now().plusWeeks(REFRESH_TOKEN_EXP_DAYS);

        String token = Jwts.builder()
                .setSubject(user.getUserName())
                .setId(jti)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(expiration.atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(keyRefresh)
                .compact();

        RefreshToken refreshToken = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .expirationTime(expiration)
                .build();
        jwtRepository.save(refreshToken);
        return token;
    }

    @Transactional
    public TokenDto refresh(String refreshToken) {
        String processedToken = refreshToken.startsWith("Bearer") ? refreshToken.substring(7) : refreshToken;

        RefreshToken token = jwtRepository.findById(extractJti(processedToken))
                .orElseThrow(() -> new BadCredentialsException("Refresh token not found"));


        if (token.isRevoked()) {
            throw new BadCredentialsException("Token revoked");
        }

        if (token.getExpirationTime().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Token expired");
        }

        token.setRevoked(true);

        String newAccessToken = generateAccessToken(token.getUser().getUserName());
        String newRefreshToken = generateRefreshToken(token.getUser());

        return TokenDto.builder().refreshToken(newRefreshToken).accessToken(newAccessToken).build();
    }

    public TokenDto newTokens(User user) {
        String newAccessToken = generateAccessToken(user.getUserName());
        String newRefreshToken = generateRefreshToken(user);

        return TokenDto.builder().refreshToken(newRefreshToken).accessToken(newAccessToken).build();
    }


}
