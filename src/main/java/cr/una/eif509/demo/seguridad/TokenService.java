package cr.una.eif509.demo.seguridad;

import cr.una.eif509.demo.model.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

// Paso 2 de la lámina: emisión del token al iniciar sesión.
@Service
public class TokenService {

    // Vigencia corta (una hora): un token emitido no se puede anular antes
    // de que expire, porque el servidor no guarda sesiones.
    public static final Duration VIGENCIA = Duration.ofHours(1);

    private final JwtEncoder encoder;

    public TokenService(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    public String emitir(Usuario u) {
        Instant ahora = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("eif509")
                .subject(u.getCorreo())                 // quién es
                .issuedAt(ahora)
                .expiresAt(ahora.plus(VIGENCIA))        // hasta cuándo vale
                .claim("roles", List.of(u.getRol()))    // qué puede hacer
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
