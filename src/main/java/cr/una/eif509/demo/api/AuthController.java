package cr.una.eif509.demo.api;

import cr.una.eif509.demo.dto.LoginRequest;
import cr.una.eif509.demo.dto.TokenResponse;
import cr.una.eif509.demo.excepcion.CredencialesInvalidasException;
import cr.una.eif509.demo.repository.UsuarioRepository;
import cr.una.eif509.demo.seguridad.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// Inicio de sesión de la API: único endpoint público. Si el correo y la
// clave son correctos, emite un token firmado.
@RestController
@Tag(name = "Autenticación", description = "Inicio de sesión y emisión del token JWT")
public class AuthController {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;

    public AuthController(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, TokenService tokens) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    @Operation(summary = "Iniciar sesión",
            description = "Devuelve un token JWT con una vigencia de una hora. Úsenlo en la cabecera "
                    + "Authorization: Bearer <token>.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciales correctas; incluye el token"),
            @ApiResponse(responseCode = "400", description = "Formato inválido",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Correo o clave incorrectos",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        // La clave recibida se compara con el hash BCrypt guardado.
        var usuario = usuarios.findByCorreo(req.correo())
                .filter(u -> passwordEncoder.matches(req.clave(), u.getClave()))
                .orElseThrow(CredencialesInvalidasException::new);   // -> 401
        return new TokenResponse(tokens.emitir(usuario), "Bearer", TokenService.VIGENCIA.toSeconds());
    }
}
