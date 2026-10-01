package cr.una.eif509.demo;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

// Configuración común de las pruebas de integración, escrita una sola vez
// (Sesión 6).
//
// Patrón «singleton container»: un solo PostgreSQL 16 real para todas las
// pruebas, iniciado en el bloque static. ¿Por qué no un @Container por
// clase? Spring mantiene en caché el ApplicationContext entre clases de
// prueba con la misma configuración, y ese contexto conserva la URL del
// contenedor. Con un contenedor por clase, la segunda clase reutilizaría el
// contexto apuntando a un contenedor que ya se detuvo, y fallaría con
// «Could not open JPA EntityManager». Testcontainers (ryuk) detiene el
// contenedor al terminar la JVM.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class PostgresContainerBase {

    static final PostgreSQLContainer<?> db =
            new PostgreSQLContainer<>("postgres:16");

    static {
        db.start();
    }

    // Le pasa a Spring la URL del contenedor, con su puerto aleatorio.
    // Al iniciar, Flyway ejecuta dentro del contenedor las migraciones
    // reales del proyecto (V1 a V6).
    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", db::getJdbcUrl);
        r.add("spring.datasource.username", db::getUsername);
        r.add("spring.datasource.password", db::getPassword);
    }
}
