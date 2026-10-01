package cr.una.eif509.demo.api;

import cr.una.eif509.demo.excepcion.InventarioInsuficienteException;
import cr.una.eif509.demo.excepcion.MontoFacturaExcedidoException;
import cr.una.eif509.demo.excepcion.PedidoNoExisteException;
import cr.una.eif509.demo.excepcion.PedidoYaConfirmadoException;
import cr.una.eif509.demo.excepcion.ProductoDuplicadoException;
import cr.una.eif509.demo.excepcion.ProductoNoExisteException;
import cr.una.eif509.demo.excepcion.ReferenciaInvalidaException;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

// Manejador de errores para toda la API (RFC 9457, Problem Details).
// Cada excepción de negocio definida en la Sesión 7 se asocia aquí, una
// sola vez, con su código HTTP. No se devuelve un 500 con la traza ni un
// 200 con {error: true}: el código HTTP es parte del contrato.
//
// Sesión 10: el manejador se limita al paquete de la API
// (basePackageClasses). Las vistas MVC tienen su propio manejador de
// errores (web/ManejadorErroresWeb), porque informan los errores con una
// página HTML y no con un cuerpo application/problem+json.
//
// @Profile("!sin-manejador") existe solo para la demo de la Sesión 9: al
// arrancar con --spring.profiles.active=sin-manejador este bean no se crea
// y se ve lo que pasa sin él (un 500 con la traza al cliente).
@RestControllerAdvice(basePackageClasses = ManejadorErrores.class)
@Profile("!sin-manejador")
public class ManejadorErrores {

    // type: una URI que identifica la clase de problema. No tiene que
    // corresponder a una página existente, aunque es buena práctica que
    // apunte a su documentación.
    private static final String TIPOS = "https://api.ejemplo.cr/errores/";

    // 422 Unprocessable Content: la petición está bien formada, pero una
    // regla de negocio la rechaza.
    @ExceptionHandler(InventarioInsuficienteException.class)
    ProblemDetail inventario(InventarioInsuficienteException e) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Inventario insuficiente",
                "inventario-insuficiente", e);
    }

    @ExceptionHandler(MontoFacturaExcedidoException.class)
    ProblemDetail montoExcedido(MontoFacturaExcedidoException e) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Monto de facturación excedido",
                "monto-excedido", e);
    }

    // 422 también cuando el cuerpo referencia algo que no existe: la URL
    // del recurso es válida; lo inválido es el contenido.
    @ExceptionHandler(ReferenciaInvalidaException.class)
    ProblemDetail referenciaInvalida(ReferenciaInvalidaException e) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Referencia inválida",
                "referencia-invalida", e);
    }

    // 409 Conflict: el estado actual del recurso impide la operación.
    @ExceptionHandler(PedidoYaConfirmadoException.class)
    ProblemDetail yaConfirmado(PedidoYaConfirmadoException e) {
        return problema(HttpStatus.CONFLICT, "Pedido ya confirmado",
                "pedido-ya-confirmado", e);
    }

    // 409 Conflict: el nombre ya está en uso por otro producto.
    @ExceptionHandler(ProductoDuplicadoException.class)
    ProblemDetail productoDuplicado(ProductoDuplicadoException e) {
        return problema(HttpStatus.CONFLICT, "Producto duplicado",
                "producto-duplicado", e);
    }

    // 404 Not Found: el recurso de la URL no existe.
    @ExceptionHandler(PedidoNoExisteException.class)
    ProblemDetail noExiste(PedidoNoExisteException e) {
        return problema(HttpStatus.NOT_FOUND, "Pedido no encontrado",
                "pedido-no-existe", e);
    }

    @ExceptionHandler(ProductoNoExisteException.class)
    ProblemDetail productoNoExiste(ProductoNoExisteException e) {
        return problema(HttpStatus.NOT_FOUND, "Producto no encontrado",
                "producto-no-existe", e);
    }

    // 400 Bad Request: falló @Valid (formato). Spring ya respondería 400
    // sin este método; aquí se agrega el detalle por campo, en el mismo
    // formato estándar, para que el cliente sepa qué debe corregir.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail formato(MethodArgumentNotValidException e) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "La petición no cumple el formato esperado");
        pd.setTitle("Formato inválido");
        pd.setType(URI.create(TIPOS + "validacion"));
        Map<String, String> errores = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> errores.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        pd.setProperty("errores", errores);
        return pd;
    }

    // 400 cuando sort= nombra un campo que la entidad no tiene
    // (por ejemplo sort=fecha en vez de sort=creadoEn).
    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail ordenamiento(PropertyReferenceException e) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "El campo de ordenamiento «" + e.getPropertyName() + "» no existe en el recurso");
        pd.setTitle("Parámetro de ordenamiento inválido");
        pd.setType(URI.create(TIPOS + "ordenamiento-invalido"));
        return pd;
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String tipo,
                                          RuntimeException e) {
        var pd = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        pd.setTitle(titulo);
        pd.setType(URI.create(TIPOS + tipo));
        return pd;   // Spring responde con Content-Type: application/problem+json
    }
}
