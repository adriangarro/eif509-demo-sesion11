package cr.una.eif509.demo.api;

import cr.una.eif509.demo.dto.ProductoForm;
import cr.una.eif509.demo.dto.ProductoResumen;
import cr.una.eif509.demo.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

// La presentación para otros sistemas (el patrón de la Sesión 9):
// @RestController devuelve datos, que Spring serializa como JSON.
// La presentación equivalente para personas es web/ProductoWeb, que usa el
// mismo servicio.
@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "Productos", description = "Catálogo de productos (contrato v1)")
public class ProductoApi {

    private static final String PROBLEMA = "application/problem+json";

    private final ProductoService service;

    public ProductoApi(ProductoService service) {
        this.service = service;
    }

    @Operation(summary = "Listar productos (paginado y ordenado)",
            description = "Por defecto, los productos se ordenan del más reciente al más antiguo. El parámetro "
                    + "sort usa el nombre del campo de la entidad, por ejemplo sort=producto,asc o sort=precio,desc.")
    @GetMapping
    public PagedModel<ProductoResumen> listar(
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(service.listar(pageable));   // la misma línea que en ProductoWeb
    }

    @Operation(summary = "Obtener un producto por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "El producto"),
            @ApiResponse(responseCode = "404", description = "El producto no existe",
                    content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public ProductoResumen obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Crear un producto",
            description = "Recibe el mismo DTO que el formulario HTML del módulo administrativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto creado; la cabecera Location contiene la URL del recurso"),
            @ApiResponse(responseCode = "400", description = "Formato inválido: falló la validación de @Valid",
                    content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe un producto con ese nombre",
                    content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<ProductoResumen> crear(@Valid @RequestBody ProductoForm form) {
        var creado = service.crear(form);
        return ResponseEntity.created(URI.create("/api/v1/productos/" + creado.id())).body(creado);
    }
}
