package cr.una.eif509.demo.seguridad;

import cr.una.eif509.demo.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Usuarios del formulario de inicio de sesión de las vistas (Sesión 10).
// Desde esta sesión salen de la misma tabla que usa la API.
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarios;

    public UsuarioDetailsService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) {
        return usuarios.findByCorreo(correo)
                .map(u -> User.withUsername(u.getCorreo())
                        .password(u.getClave())
                        .roles(u.getRol())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(correo));
    }
}
