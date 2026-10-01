package cr.una.eif509.demo;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
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
                + "en formato Problem Details (RFC 9457). Para probar los endpoints protegidos, "
                + "obtengan un token con POST /auth/login y péguenlo en el botón «Authorize»."),
        security = @SecurityRequirement(name = "bearer"))
// Esquema de seguridad para Swagger UI: agrega el botón «Authorize», donde se
// pega el token, y lo envía en la cabecera Authorization: Bearer <token>.
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
