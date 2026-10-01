package cr.una.eif509.demo.dto;

// Respuesta del inicio de sesión: el token y su vigencia en segundos.
public record TokenResponse(String token, String tipo, long expiraEnSegundos) {
}
