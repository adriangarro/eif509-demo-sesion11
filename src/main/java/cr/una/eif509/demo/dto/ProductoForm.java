package cr.una.eif509.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// DTO de entrada del catálogo, compartido por las dos presentaciones:
//   - la API (POST /api/v1/productos) lo recibe como JSON;
//   - la vista MVC (POST /admin/productos) lo recibe del formulario HTML.
// Ambas aplican las mismas reglas de formato (Bean Validation, Sesión 7);
// lo único que cambia es la forma de informar el error: 400 Problem
// Details en la API y mensajes junto a cada campo en el formulario.
//
// Es una clase y no un record porque el formulario necesita un objeto
// vacío para GET /nuevo y métodos set para enlazar los campos (th:field).
public class ProductoForm {

    @Schema(description = "Nombre del producto (único en el catálogo)", example = "Café Tarrazú 1 kg")
    @NotBlank(message = "Este campo es obligatorio.")
    @Size(max = 120, message = "El nombre no puede superar 120 caracteres.")
    private String nombre;

    @Schema(description = "Precio unitario en colones", example = "9800.00")
    @NotNull(message = "Este campo es obligatorio.")
    @Positive(message = "El valor debe ser mayor que cero.")
    @Digits(integer = 10, fraction = 2, message = "El precio admite como máximo 2 decimales.")
    private BigDecimal precio;

    @Schema(description = "Existencias disponibles", example = "25")
    @NotNull(message = "Este campo es obligatorio.")
    @PositiveOrZero(message = "El valor no puede ser negativo.")
    @Max(value = 100000, message = "El valor no puede superar 100 000.")
    private Integer disponible;

    public ProductoForm() {
        // Formulario vacío para GET /admin/productos/nuevo.
    }

    public ProductoForm(String nombre, BigDecimal precio, Integer disponible) {
        this.nombre = nombre;
        this.precio = precio;
        this.disponible = disponible;
    }

    // Para el formulario de edición: se construye a partir de los datos que
    // devuelve el servicio.
    public static ProductoForm desde(ProductoResumen p) {
        return new ProductoForm(p.nombre(), p.precio(), p.disponible());
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public Integer getDisponible() {
        return disponible;
    }

    public void setDisponible(Integer disponible) {
        this.disponible = disponible;
    }
}
