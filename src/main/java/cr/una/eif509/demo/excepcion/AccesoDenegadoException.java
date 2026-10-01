package cr.una.eif509.demo.excepcion;

// El usuario está autenticado, pero el recurso pertenece a otro usuario
// (OWASP API1: autorización a nivel de objeto).
public class AccesoDenegadoException extends ExcepcionDeNegocio {

    public AccesoDenegadoException() {
        super("No tiene permiso para consultar este recurso");
    }
}
