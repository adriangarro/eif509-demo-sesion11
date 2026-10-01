package cr.una.eif509.demo;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Sesión 9: la aplicación es una API REST. springdoc genera la
// especificación OpenAPI desde el código (controladores, DTOs y códigos de
// respuesta) y la publica en /v3/api-docs y en Swagger UI.
@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "EIF509 · API de pedidos",
        version = "v1",
        description = "Contrato público del sistema de pedidos del curso: recursos REST, "
                + "métodos HTTP usados según su semántica, códigos de estado correctos y errores "
                + "en formato Problem Details (RFC 9457)."))
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
