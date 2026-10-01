package cr.una.eif509.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// DTO de entrada: contiene solo los datos que necesita el caso de uso.
// Nivel 1 de validación (Sesión 7): el formato, de forma declarativa con
// Bean Validation; el controlador la activa con @Valid antes de llamar al
// servicio. La validación de negocio (¿existe el cliente?, ¿hay
// existencias?) se hace en el servicio.
public record CrearPedidoRequest(

        @Schema(description = "Identificador del cliente que hace el pedido", example = "1")
        @NotNull(message = "Este campo es obligatorio.")
        Long clienteId,

        @Schema(description = "Identificador del producto en el inventario", example = "1")
        @NotNull(message = "Este campo es obligatorio.")
        Long productoId,

        @Schema(description = "Unidades solicitadas (1 a 1000)", example = "2")
        @NotNull(message = "Este campo es obligatorio.")
        @Positive(message = "El valor debe ser mayor que cero.")
        @Max(value = 1000, message = "El valor no puede superar 1000.")
        Integer cantidad) {
}
