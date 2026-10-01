package cr.una.eif509.demo.service;

import cr.una.eif509.demo.model.IntentoConfirmacion;
import cr.una.eif509.demo.repository.IntentoConfirmacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {

    private final IntentoConfirmacionRepository intentos;

    public AuditoriaService(IntentoConfirmacionRepository intentos) {
        this.intentos = intentos;
    }

    // REQUIRES_NEW: suspende la transacción actual y abre una nueva e
    // independiente. Se confirma (commit) por separado, así que el registro
    // se conserva aunque confirmarPedido haga rollback.
    //
    // Debe estar en otro bean: @Transactional funciona mediante un proxy, y
    // una llamada a un método de la misma clase no pasa por el proxy.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarIntento(Long pedidoId, String resultado) {
        intentos.save(new IntentoConfirmacion(pedidoId, resultado));
    }
}
