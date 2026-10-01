package cr.una.eif509.demo.excepcion;

import java.math.BigDecimal;

public class MontoFacturaExcedidoException extends ExcepcionDeNegocio {

    public MontoFacturaExcedidoException(Long pedidoId, BigDecimal monto, BigDecimal limite) {
        super("El pedido " + pedidoId + ", por un monto de " + monto
                + ", supera el límite de facturación automática (" + limite
                + ") y requiere aprobación manual");
    }
}
