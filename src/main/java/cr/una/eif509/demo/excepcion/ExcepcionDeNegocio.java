package cr.una.eif509.demo.excepcion;

// Un error de negocio no es una excepción técnica: es una respuesta
// esperada del dominio y tiene un nombre propio. Extiende RuntimeException
// porque @Transactional hace rollback automático ante cualquier
// RuntimeException; por eso, cuando se viola una regla, se deshace el
// proceso completo.
public abstract class ExcepcionDeNegocio extends RuntimeException {

    protected ExcepcionDeNegocio(String mensaje) {
        super(mensaje);
    }
}
