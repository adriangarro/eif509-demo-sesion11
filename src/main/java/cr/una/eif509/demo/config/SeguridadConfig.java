package cr.una.eif509.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

// La aplicación tiene dos presentaciones con modelos de seguridad
// distintos, por lo que declara una SecurityFilterChain para cada una.
@Configuration
public class SeguridadConfig {

    // Cadena 1 · La API (/api/**): sin estado y sin CSRF, porque no hay
    // cookie de sesión que un sitio ajeno pueda explotar. En esta demo la
    // API no exige autenticación; en el Laboratorio 5, aquí se agregan el
    // filtro JWT (.oauth2ResourceServer(...)) y la autorización por rol.
    @Bean
    @Order(1)
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .build();
    }

    // Cadena 2 · Las vistas (el resto de las rutas): sesión con formulario
    // de inicio de sesión y protección CSRF habilitada (valor por defecto).
    // Thymeleaf inserta el token en cada formulario que usa th:action; un
    // POST sin el token recibe 403.
    @Bean
    @Order(2)
    SecurityFilterChain web(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().permitAll())   // /login, /css, Swagger, /error
                .formLogin(f -> f
                        .loginPage("/login")
                        .defaultSuccessUrl("/admin/productos"))
                .logout(l -> l.logoutSuccessUrl("/login?salida"))
                .build();
    }

    // Usuario de demostración en memoria (rol ADMIN). En un sistema real,
    // los usuarios se almacenan en la base de datos con la contraseña cifrada.
    @Bean
    UserDetailsService usuarios(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(User.withUsername("admin")
                .password(encoder.encode("admin123"))
                .roles("ADMIN")
                .build());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
