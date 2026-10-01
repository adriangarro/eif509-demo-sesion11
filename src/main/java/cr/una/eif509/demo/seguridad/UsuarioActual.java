package cr.una.eif509.demo.seguridad;

import org.springframework.security.core.Authentication;

// Lo que el servicio necesita saber del usuario autenticado para verificar
// la propiedad de un recurso. El controlador lo construye a partir del
// token; así el servicio no depende de Spring Security.
public record UsuarioActual(String correo, boolean esAdmin) {

    // getName() devuelve el sub del token (el correo); las autoridades
    // vienen de la claim "roles" (ROLE_ADMIN, ROLE_CLIENTE).
    public static UsuarioActual de(Authentication auth) {
        boolean admin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return new UsuarioActual(auth.getName(), admin);
    }
}
