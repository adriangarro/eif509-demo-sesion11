package cr.una.eif509.demo.excepcion;

public class ProductoNoExisteException extends ExcepcionDeNegocio {

    public ProductoNoExisteException(Long productoId) {
        super("No existe el producto " + productoId);
    }
}
