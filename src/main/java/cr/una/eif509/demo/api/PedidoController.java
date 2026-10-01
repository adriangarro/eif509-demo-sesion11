package cr.una.eif509.demo.api;

import cr.una.eif509.demo.dto.CrearPedidoRequest;
import cr.una.eif509.demo.dto.PedidoResumen;
import cr.una.eif509.demo.model.EstadoPedido;
import cr.una.eif509.demo.seguridad.UsuarioActual;
import cr.una.eif509.demo.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

// El controlador no contiene lógica de negocio: recibe la petición con un
// DTO validado, llama al servicio y devuelve un DTO con el código de estado
// correcto. Tampoco atrapa excepciones: ManejadorErrores las traduce para
// toda la API.
//
// La versión va en la ruta desde el inicio (/api/v1). Un contrato publicado
// no debe cambiar de forma incompatible; si hace falta un cambio así, se
// publica /api/v2 y la v1 sigue disponible.
@RestController
@RequestMapping("/api/v1/pedidos")
@Tag(name = "Pedidos", description = "Recurso principal del sistema de pedidos (contrato v1)")
public class PedidoController {

    private static final String PROBLEMA = "application/problem+json";

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @Operation(summary = "Crear un pedido",
            description = "Valida el formato (@Valid), delega al servicio y responde 201 con la "
                    + "cabecera Location del recurso nuevo. El total se calcula en el servidor.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido creado; la cabecera Location contiene la URL del recurso"),
            @ApiResponse(responseCode = "400", description = "Formato inválido: falló la validación de @Valid",
                    content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Una regla de negocio rechazó el pedido: no hay existencias "
                    + "suficientes, o el cliente o el producto no existen",
                    content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<PedidoResumen> crear(@Valid @RequestBody CrearPedidoRequest req) {
        var resumen = service.crear(req);
        return ResponseEntity
                .created(URI.create("/api/v1/pedidos/" + resumen.id()))
                .body(resumen);
    }

    @Operation(summary = "Obtener un pedido por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "El pedido"),
            @ApiResponse(responseCode = "401", description = "Falta el token o no es válido",
                    content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "El pedido pertenece a otro cliente",
                    content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "El pedido no existe",
                    content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public PedidoResumen obtener(@PathVariable Long id, Authentication auth) {
        // auth es el usuario del token; el servicio verifica la propiedad.
        return service.obtener(id, UsuarioActual.de(auth));
    }

    @Operation(summary = "Listar pedidos (paginado, ordenado y filtrado)",
            description = "Un CLIENTE recibe solo sus pedidos; ADMIN recibe todos. "
                    + "La colección siempre se entrega paginada: si no se indican page y size, "
                    + "se devuelve la primera página con 20 pedidos. El parámetro sort usa el nombre "
                    + "del campo de la entidad, por ejemplo sort=creadoEn,desc.")
    @ApiResponse(responseCode = "200", description = "Una página de resultados: content y los metadatos de paginación (page)")
    @GetMapping
    public PagedModel<PedidoResumen> listar(
            @Parameter(description = "Filtro opcional por estado") @RequestParam(required = false) EstadoPedido estado,
            @ParameterObject @PageableDefault(size = 20, sort = "creadoEn", direction = Sort.Direction.DESC)
            Pageable pageable,
            Authentication auth) {
        // PagedModel es la forma estable que recomienda Spring Data 3.3 para
        // exponer una Page: { content: [...], page: { size, number,
        // totalElements, totalPages } }. Con esos metadatos, el cliente sabe
        // en qué página está y cuántas hay.
        return new PagedModel<>(service.listar(estado, pageable, UsuarioActual.de(auth)));
    }

    @Operation(summary = "Confirmar un pedido",
            description = "Acción de negocio que no corresponde a una operación CRUD; se modela como un "
                    + "subrecurso de transición. Reserva el inventario, crea la factura y registra la "
                    + "bitácora en una sola transacción: se completan todos los pasos o ninguno (Sesión 7).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido confirmado"),
            @ApiResponse(responseCode = "404", description = "El pedido no existe",
                    content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "El pedido ya estaba confirmado",
                    content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Una regla de negocio rechazó la confirmación: no hay existencias "
                    + "suficientes o el monto supera el límite de facturación automática",
                    content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{id}/confirmacion")
    public PedidoResumen confirmar(@PathVariable Long id) {
        return service.confirmar(id);
    }
}
