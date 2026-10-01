package cr.una.eif509.demo.dto;

import java.math.BigDecimal;

// DTO de salida del catálogo: se usa tanto en el JSON de la API como en el
// modelo de la vista. Usa el nombre que muestra la pantalla (nombre) y no el
// de la columna de la tabla (producto); esa separación es la frontera DTO de
// la Sesión 8.
public record ProductoResumen(Long id, String nombre, BigDecimal precio, int disponible) {
}
