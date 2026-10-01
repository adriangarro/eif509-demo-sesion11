package cr.una.eif509.demo.model;

import cr.una.eif509.demo.excepcion.InventarioInsuficienteException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

@Entity
@Table(name = "inventario")
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String producto;

    @Column(nullable = false)
    private int disponible;

    // Sesión 9: precio unitario; el total del pedido se calcula con él.
    @Column(nullable = false)
    private BigDecimal precio;

    // Bloqueo optimista: Hibernate genera
    //   UPDATE inventario SET disponible=?, version=N+1 WHERE id=? AND version=N
    // Si otra transacción modificó la fila antes (0 filas afectadas), lanza
    // OptimisticLockException en lugar de sobrescribir ese cambio.
    @Version
    private Long version;

    protected Inventario() {
        // Constructor sin argumentos requerido por JPA.
    }

    public Inventario(String producto, int disponible, BigDecimal precio) {
        this.producto = producto;
        this.disponible = disponible;
        this.precio = precio;
    }

    // Invariante simple: según el criterio del curso, se implementa en la
    // entidad. El CHECK (disponible >= 0) del esquema es una segunda defensa
    // en la base de datos.
    public void verificarDisponible(int cantidad) {
        if (disponible < cantidad) {
            throw new InventarioInsuficienteException(producto, disponible, cantidad);
        }
    }

    public void reservar(int cantidad) {
        verificarDisponible(cantidad);
        disponible -= cantidad;
    }

    // Sesión 10: el módulo administrativo edita los datos del catálogo.
    public void actualizarDatos(String producto, BigDecimal precio, int disponible) {
        this.producto = producto;
        this.precio = precio;
        this.disponible = disponible;
    }

    // Cálculo de negocio con nombre: precio × cantidad.
    public BigDecimal precioPor(int cantidad) {
        return precio.multiply(BigDecimal.valueOf(cantidad));
    }

    public Long getId() {
        return id;
    }

    public String getProducto() {
        return producto;
    }

    public int getDisponible() {
        return disponible;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public Long getVersion() {
        return version;
    }
}
