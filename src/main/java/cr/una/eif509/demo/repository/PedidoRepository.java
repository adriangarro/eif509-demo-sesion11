package cr.una.eif509.demo.repository;

import cr.una.eif509.demo.model.EstadoPedido;
import cr.una.eif509.demo.model.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Se aplica lo visto en la Sesión 5: el DTO de salida necesita el nombre
    // del cliente y del producto, y el grafo de entidades los carga en una
    // sola consulta (sin N+1), tanto para un pedido como para una página
    // completa.
    @Override
    @EntityGraph(attributePaths = {"cliente", "producto"})
    Optional<Pedido> findById(Long id);

    // Pageable llega desde la URL (page, size, sort) y se pasa hasta el
    // repositorio: Spring Data genera el LIMIT/OFFSET y la consulta de conteo.
    @Override
    @EntityGraph(attributePaths = {"cliente", "producto"})
    Page<Pedido> findAll(Pageable pageable);

    // Filtro explícito por estado: método derivado del nombre.
    @EntityGraph(attributePaths = {"cliente", "producto"})
    Page<Pedido> findByEstado(EstadoPedido estado, Pageable pageable);

    // Sesión 11: los pedidos de un cliente (por su correo), con y sin
    // filtro por estado. Spring Data navega cliente.email.
    @EntityGraph(attributePaths = {"cliente", "producto"})
    Page<Pedido> findByClienteEmail(String email, Pageable pageable);

    @EntityGraph(attributePaths = {"cliente", "producto"})
    Page<Pedido> findByClienteEmailAndEstado(String email, EstadoPedido estado, Pageable pageable);
}
