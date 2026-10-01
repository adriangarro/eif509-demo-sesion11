package cr.una.eif509.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Usuario de la aplicación. La clave se almacena como hash BCrypt.
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String correo;

    @Column(nullable = false)
    private String clave;

    // ADMIN o CLIENTE (restricción CHECK en el esquema).
    @Column(nullable = false)
    private String rol;

    protected Usuario() {
        // Constructor sin argumentos requerido por JPA.
    }

    public Long getId() {
        return id;
    }

    public String getCorreo() {
        return correo;
    }

    public String getClave() {
        return clave;
    }

    public String getRol() {
        return rol;
    }
}
