package cr.una.eif509.demo.excepcion;

// El mismo mensaje si el correo no existe o si la clave es incorrecta: así
// no se revela qué correos están registrados.
public class CredencialesInvalidasException extends ExcepcionDeNegocio {

    public CredencialesInvalidasException() {
        super("Correo o clave incorrectos");
    }
}
