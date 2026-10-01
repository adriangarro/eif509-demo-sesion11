package cr.una.eif509.demo.excepcion;

// Regla de negocio del catálogo: no puede haber dos productos con el
// mismo nombre (sin distinguir mayúsculas). La restricción UNIQUE del
// esquema es una segunda defensa en la base de datos; esta excepción
// permite informar la regla con un nombre del dominio.
public class ProductoDuplicadoException extends ExcepcionDeNegocio {

    public ProductoDuplicadoException(String nombre) {
        super("Ya existe un producto con el nombre «" + nombre + "»");
    }
}
