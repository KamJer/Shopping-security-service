package pl.kamjer.ShoppingSecService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // dla wszystkich endpointów
                        .allowedOrigins("*") // lub konkretne domeny
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("Origin", "Accept", "Content-Type", "X-Requested-With", "X-XSS-Protection")
                        .exposedHeaders("Access-Control-Allow-Origin", "X-XSS-Protection")
                        .maxAge(3600);
            }
        };
    }
}
