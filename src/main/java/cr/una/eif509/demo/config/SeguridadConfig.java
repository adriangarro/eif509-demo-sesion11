package cr.una.eif509.demo.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

// La aplicación tiene dos presentaciones con modelos de seguridad
// distintos, por lo que declara una SecurityFilterChain para cada una.
@Configuration
public class SeguridadConfig {

    // Paso 1 de la lámina: la clave se lee de la variable de entorno
    // JWT_SECRETO (ver application.properties) y nunca se escribe en el código.
    // El mismo secreto firma y verifica el token (HS256).
    @Bean
    SecretKey claveJwt(@Value("${jwt.secreto}") String secreto) {
        // HS256 requiere una clave de al menos 256 bits (32 bytes). Se valida
        // al arrancar para que el error aparezca de inmediato y con un mensaje
        // claro, y no al emitir el primer token.
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRETO debe tener al menos 32 caracteres "
                    + "(tiene " + bytes.length + "). HS256 requiere una clave de 256 bits.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey clave) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey clave) {
        // Verifica la firma y la expiración del token en cada petición.
        return NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
    }

    // Las claves se guardan con BCrypt, nunca en texto plano.
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Cadena 1 · La API (/api/** y /auth/**): sin estado (STATELESS) y sin
    // CSRF, porque no hay cookie de sesión que un sitio ajeno pueda
    // explotar. Cada petición se autentica con el token de la cabecera
    // Authorization: Bearer <token>.
    @Bean
    @Order(1)
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/**", "/auth/**")
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())   // usa el bean corsConfigurationSource
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/auth/login").permitAll()
                        // Paso 3: autorización por rol. Solo ADMIN crea productos.
                        .requestMatchers(HttpMethod.POST, "/api/v1/productos/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(convertidorDeRoles()))
                        .authenticationEntryPoint(RespuestasDeSeguridad.noAutenticado()))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(RespuestasDeSeguridad.noAutenticado())
                        .accessDeniedHandler(RespuestasDeSeguridad.sinPermiso()))
                .build();
    }

    // Convertidor: transforma la claim "roles" del token en las autoridades
    // que usa Spring (ROLE_ADMIN, ROLE_CLIENTE), para que hasRole("ADMIN")
    // funcione. Sin él, hasRole siempre responde 403.
    static JwtAuthenticationConverter convertidorDeRoles() {
        var roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        var convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(roles);
        return convertidor;
    }

    // CORS: la SPA (http://localhost:5173) y la API (http://localhost:8080)
    // son orígenes distintos. El navegador solo entrega la respuesta a la
    // SPA si la API autoriza ese origen de forma explícita. Se autorizan
    // orígenes concretos, nunca "*". El valor sale de cors.origenes.
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${cors.origenes:}") String[] origenes) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origenes).map(String::trim).filter(o -> !o.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Location"));
        var fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", config);
        fuente.registerCorsConfiguration("/auth/**", config);
        return fuente;
    }

    // Cadena 2 · Las vistas (el resto de las rutas, Sesión 10): sesión con
    // formulario de inicio de sesión y protección CSRF habilitada. Desde
    // esta sesión, los usuarios salen de la tabla usuario
    // (UsuarioDetailsService) y /admin/** exige el rol ADMIN.
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
}
