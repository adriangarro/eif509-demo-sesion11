package cr.una.eif509.demo.dto;

import cr.una.eif509.demo.model.Pedido;

// Mapeo manual de entidad a DTO: explícito y sin dependencias.
// Criterio del curso: mapeo manual para uno o dos DTOs y MapStruct cuando
// la cantidad de DTOs crece (MapStruct genera este mismo código al compilar).
public final class PedidoMapper {

    private PedidoMapper() {
    }

    public static PedidoResumen aResumen(Pedido p) {
        return new PedidoResumen(
                p.getId(),
                p.getCliente().getNombre(),
                p.getProducto().getProducto(),
                p.getTotal(),
                p.getEstado());
    }
}
