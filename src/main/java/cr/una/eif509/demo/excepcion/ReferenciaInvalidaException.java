package cr.una.eif509.demo.excepcion;

// El cuerpo de una petición hace referencia a algo que no existe (por
// ejemplo, un pedido para el cliente 99). La URL es válida; lo inválido es
// el dato. Es una situación distinta de «el recurso de la URL no existe»
// (ProductoNoExiste, PedidoNoExiste) y por eso se traduce a otro código HTTP.
public class ReferenciaInvalidaException extends ExcepcionDeNegocio {

    public ReferenciaInvalidaException(String recurso, Long id) {
        super("No existe el " + recurso + " " + id);
    }
}
