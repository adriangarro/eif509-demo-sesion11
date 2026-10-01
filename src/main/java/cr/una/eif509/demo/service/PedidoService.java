package cr.una.eif509.demo.service;

import cr.una.eif509.demo.dto.CrearPedidoRequest;
import cr.una.eif509.demo.dto.PedidoMapper;
import cr.una.eif509.demo.dto.PedidoResumen;
import cr.una.eif509.demo.excepcion.PedidoNoExisteException;
import cr.una.eif509.demo.excepcion.PedidoYaConfirmadoException;
import cr.una.eif509.demo.excepcion.ReferenciaInvalidaException;
import cr.una.eif509.demo.model.EstadoPedido;
import cr.una.eif509.demo.model.Pedido;
import cr.una.eif509.demo.repository.ClienteRepository;
import cr.una.eif509.demo.repository.InventarioRepository;
import cr.una.eif509.demo.repository.PedidoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// La capa de servicios: aquí están las reglas de negocio y la orquestación
// del proceso. Sus métodos usan términos del negocio (crear, confirmar) y
// no de HTTP ni de la base de datos. Recibe DTOs de entrada y devuelve DTOs
// de salida: la entidad JPA no cruza la frontera (Sesión 8).
@Service
public class PedidoService {

    private final PedidoRepository pedidos;
    private final ClienteRepository clientes;
    private final InventarioRepository inventarios;
    private final FacturaService facturas;
    private final BitacoraService bitacora;
    private final AuditoriaService auditoria;

    // Inyección por constructor: dependencias explícitas, finales
    // y fáciles de reemplazar en una prueba.
    public PedidoService(PedidoRepository pedidos,
                         ClienteRepository clientes,
                         InventarioRepository inventarios,
                         FacturaService facturas,
                         BitacoraService bitacora,
                         AuditoriaService auditoria) {
        this.pedidos = pedidos;
        this.clientes = clientes;
        this.inventarios = inventarios;
        this.facturas = facturas;
        this.bitacora = bitacora;
        this.auditoria = auditoria;
    }

    // Sesión 9 · Caso de uso «crear pedido». El DTO llega con el formato ya
    // validado (@Valid en el controlador); aquí se aplican las reglas de
    // negocio: que el cliente y el producto existan, que haya existencias, y
    // el cálculo del total (precio × cantidad), que nunca envía el cliente.
    @Transactional
    public PedidoResumen crear(CrearPedidoRequest req) {
        // Cliente y producto llegan como referencias en el cuerpo: si no
        // existen, la petición es inválida (422), no la URL (404).
        var cliente = clientes.findById(req.clienteId())
                .orElseThrow(() -> new ReferenciaInvalidaException("cliente", req.clienteId()));
        var producto = inventarios.findById(req.productoId())
                .orElseThrow(() -> new ReferenciaInvalidaException("producto", req.productoId()));

        producto.verificarDisponible(req.cantidad());

        var pedido = pedidos.save(new Pedido(cliente, producto, req.cantidad(),
                producto.precioPor(req.cantidad())));
        return PedidoMapper.aResumen(pedido);
    }

    @Transactional(readOnly = true)
    public PedidoResumen obtener(Long pedidoId) {
        return pedidos.findById(pedidoId)
                .map(PedidoMapper::aResumen)
                .orElseThrow(() -> new PedidoNoExisteException(pedidoId));
    }

    // La colección siempre se entrega paginada. El filtro por estado es
    // explícito; page, size y sort vienen en el Pageable desde la URL.
    @Transactional(readOnly = true)
    public Page<PedidoResumen> listar(EstadoPedido estado, Pageable pageable) {
        var pagina = estado == null
                ? pedidos.findAll(pageable)
                : pedidos.findByEstado(estado, pageable);
        return pagina.map(PedidoMapper::aResumen);
    }

    // La transición de estado como caso de uso para la API: la acción de
    // negocio «confirmar» se expone como el subrecurso POST
    // /{id}/confirmacion y devuelve el DTO del recurso resultante.
    @Transactional
    public PedidoResumen confirmar(Long pedidoId) {
        return PedidoMapper.aResumen(confirmarPedido(pedidoId));
    }

    // Sesión 7 · @Transactional en el método de servicio: la transacción
    // abarca el caso de uso completo. Si cualquier paso lanza una
    // RuntimeException, los anteriores se deshacen (rollback).
    @Transactional
    public Pedido confirmarPedido(Long pedidoId) {
        var pedido = pedidos.findById(pedidoId)
                .orElseThrow(() -> new PedidoNoExisteException(pedidoId));

        // Se valida antes de modificar el estado del sistema.
        if (pedido.estaConfirmado()) {
            throw new PedidoYaConfirmadoException(pedidoId);
        }

        try {
            // Paso 1 · Reservar inventario (regla: no se vende más de lo que hay
            // en existencia).
            var inventario = pedido.getProducto();
            inventario.reservar(pedido.getCantidad());
            inventarios.saveAndFlush(inventario);

            // Paso 2 · Crear la factura (regla: límite de facturación).
            facturas.crear(pedido);

            // Paso 3 · Registrar en la bitácora del proceso.
            bitacora.registrar(pedido, "confirmado");

            pedido.confirmar();
            auditoria.registrarIntento(pedidoId, "OK");
            return pedidos.save(pedido);

        } catch (RuntimeException e) {
            // La auditoría se ejecuta con REQUIRES_NEW: se guarda aunque esta
            // transacción haga rollback. La excepción se vuelve a lanzar: el
            // controlador no la atrapa y el manejador global la traduce.
            auditoria.registrarIntento(pedidoId, "FALLO: " + e.getMessage());
            throw e;
        }
    }
}
