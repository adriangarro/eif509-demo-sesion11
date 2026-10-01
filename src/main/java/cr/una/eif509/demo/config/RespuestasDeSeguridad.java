package cr.una.eif509.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

// Los errores 401 y 403 los produce el filtro de seguridad, antes de llegar
// a los controladores; por eso no pasan por ManejadorErrores. Esta clase
// los escribe en el mismo formato Problem Details (RFC 9457), para que el
// cliente (por ejemplo, la SPA) los lea igual que los demás errores.
final class RespuestasDeSeguridad {

    private static final ObjectMapper JSON = new ObjectMapper();

    private RespuestasDeSeguridad() {
    }

    // 401: no se envió token, o es inválido o está vencido.
    static AuthenticationEntryPoint noAutenticado() {
        return (request, response, e) -> {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            escribir(request, response, HttpStatus.UNAUTHORIZED, "No autenticado",
                    "no-autenticado", "Se requiere un token válido en la cabecera Authorization.");
        };
    }

    // 403: el token es válido, pero el rol no permite usar la ruta.
    static AccessDeniedHandler sinPermiso() {
        return (request, response, e) ->
                escribir(request, response, HttpStatus.FORBIDDEN, "Acceso denegado",
                        "acceso-denegado", "Su rol no tiene permiso para realizar esta operación.");
    }

    private static void escribir(HttpServletRequest request, HttpServletResponse response,
                                 HttpStatus status, String titulo, String tipo, String detalle)
            throws IOException {
        Map<String, Object> problema = new LinkedHashMap<>();
        problema.put("type", "https://api.ejemplo.cr/errores/" + tipo);
        problema.put("title", titulo);
        problema.put("status", status.value());
        problema.put("detail", detalle);
        problema.put("instance", request.getRequestURI());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        JSON.writeValue(response.getOutputStream(), problema);
    }
}
