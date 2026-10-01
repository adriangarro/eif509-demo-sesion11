package cr.una.eif509.demo.dto;

import cr.una.eif509.demo.model.EstadoPedido;

import java.math.BigDecimal;

// DTO de salida: contiene solo los datos que la vista necesita.
// Un record de Java 21 es un DTO inmutable en una línea: constructor,
// accesores, equals y hashCode generados. La entidad JPA nunca sale de la
// capa de negocio; así se evita exponer sus relaciones LAZY y provocar un
// N+1 al serializar el JSON.
public record PedidoResumen(Long id, String cliente, String producto,
                            BigDecimal total, EstadoPedido estado) {
}
