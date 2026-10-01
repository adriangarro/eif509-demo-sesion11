package cr.una.eif509.demo.dto;

import cr.una.eif509.demo.model.Inventario;

// Mapeo manual de entidad a DTO (criterio del curso para uno o dos DTOs).
public final class ProductoMapper {

    private ProductoMapper() {
    }

    public static ProductoResumen aResumen(Inventario i) {
        return new ProductoResumen(i.getId(), i.getProducto(), i.getPrecio(), i.getDisponible());
    }
}
