# EIF509 · Demo Sesión 11 — Autenticación con JWT en la API y SPA en React

Repositorio de demostración del curso **EIF509 «Desarrollo de Aplicaciones
Basadas en Web»** (Universidad Nacional, Costa Rica).

Esta demo continúa el proyecto de la
[Sesión 10 (MVC y Thymeleaf)](https://github.com/adriangarro/eif509-demo-sesion10).
Protege la API con **tokens JWT y roles**, y agrega un **segundo frontend**:
una aplicación de una sola página (SPA) en **React** que consume la misma
API. Las demos anteriores de la serie son:
[Sesión 3](https://github.com/adriangarro/eif509-demo-sesion3) ·
[Sesión 4](https://github.com/adriangarro/eif509-demo-sesion4) ·
[Sesión 5](https://github.com/adriangarro/eif509-demo-sesion5) ·
[Sesión 6](https://github.com/adriangarro/eif509-demo-sesion6) ·
[Sesión 7](https://github.com/adriangarro/eif509-demo-sesion7) ·
[Sesión 8](https://github.com/adriangarro/eif509-demo-sesion8) ·
[Sesión 9](https://github.com/adriangarro/eif509-demo-sesion9) ·
[Sesión 10](https://github.com/adriangarro/eif509-demo-sesion10).

## ¿Qué van a construir?

**Demostración 1 · Proteger la API con JWT y roles**

1. La API sin seguridad responde a cualquiera (rama `inicio`).
2. Con la configuración de seguridad, una petición sin token recibe **401**.
3. `POST /auth/login` emite un token firmado; su contenido se lee en
   [jwt.io](https://jwt.io).
4. Con el token en la cabecera `Authorization: Bearer <token>`, la respuesta
   es **200**.
5. Un usuario con rol `CLIENTE` intenta crear un producto: **403**.
6. Un cliente intenta consultar el pedido de otro cliente: **403**
   (propiedad del recurso).
7. Las pruebas automáticas verifican 401, 403 y 200.

**Demostración 2 · Una SPA en React que consume la API**

1. Pantalla de inicio de sesión contra `POST /auth/login`.
2. Un módulo cliente (`api.js`) que agrega el token a cada petición y
   maneja el 401 en un solo lugar.
3. Listado paginado de productos con sus tres estados: cargando, datos y
   error.
4. El error de CORS en la consola del navegador y su corrección en Spring.

### Quién responde qué

| Situación | Quién la detecta | Respuesta |
|---|---|---|
| No se envía token, o es inválido o está vencido | Filtro de seguridad | 401 Unauthorized |
| El rol no permite usar la ruta | Reglas de `SeguridadConfig` | 403 Forbidden |
| El recurso pertenece a otro usuario | `PedidoService` | 403 Forbidden |
| Correo o clave incorrectos al iniciar sesión | `AuthController` y `ManejadorErrores` | 401 Unauthorized |

Todas las respuestas de error usan el formato Problem Details (RFC 9457) de
la Sesión 9, incluidas las de 401 y 403.

## Requisitos previos

| Herramienta | Versión mínima | Descarga oficial | Cómo verificar |
|---|---|---|---|
| Docker Desktop | 4.x | [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/) | `docker --version` |
| JDK (Java) | 21 | [adoptium.net](https://adoptium.net/) | `java -version` |
| Node.js (para la SPA) | 20 | [nodejs.org](https://nodejs.org/) | `node --version` |
| Git | 2.30 | [git-scm.com/downloads](https://git-scm.com/downloads) | `git --version` |
| Un cliente HTTP | — | `curl` viene con macOS, Linux y Windows 10+ | `curl --version` |

Las peticiones de la Demostración 1 también están en
[`peticiones/seguridad.http`](peticiones/seguridad.http), para el cliente
HTTP de IntelliJ IDEA o la extensión *REST Client* de VS Code. Las notas por
sistema operativo (WSL2 en Windows, el `JAVA_HOME` de Homebrew en macOS)
están en el
[README de la Sesión 5](https://github.com/adriangarro/eif509-demo-sesion5#requisitos-previos).
En macOS con Homebrew, en cada terminal que usen:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```

## Las dos ramas

| Rama | Contenido | Uso |
|---|---|---|
| `inicio` | El proyecto de la Sesión 10: la API responde sin credenciales | Punto de partida de la Demostración 1 |
| `main` | La versión completa: JWT, roles, propiedad del recurso, CORS y la SPA | Todo lo demás; respaldo si algo falla en vivo |

## Usuarios de demostración

La migración `V7__usuarios.sql` crea dos usuarios, con la clave guardada
con BCrypt (nunca en texto plano):

| Correo | Clave | Rol | Puede |
|---|---|---|---|
| `admin@demo.cr` | `admin123` | `ADMIN` | Todo: crear productos y ver cualquier pedido |
| `cliente@demo.cr` | `cliente123` | `CLIENTE` | Leer el catálogo y ver solo sus propios pedidos |

`cliente@demo.cr` es la clienta Ana Rojas de los datos semilla: los pedidos
1, 2, 6 y 9 son suyos; el pedido 3, por ejemplo, es de Luis Mora.

`admin@demo.cr` también inicia sesión en el módulo administrativo de la
Sesión 10 (`/admin/productos`): ambas presentaciones usan la misma tabla de
usuarios.

## Las demostraciones paso a paso

Esta sección resume las dos demostraciones en el orden del guion, con un
comando por paso. Las explicaciones de cada paso («Qué observar») están en
las secciones detalladas más abajo; el número entre paréntesis indica el
paso correspondiente.

### Antes de empezar

Abran Docker Desktop y esperen a que termine de iniciar. Abran dos
terminales en la carpeta del repositorio clonado:

```bash
git clone https://github.com/adriangarro/eif509-demo-sesion11.git
```

```bash
cd eif509-demo-sesion11
```

En macOS con Homebrew, ejecuten en ambas terminales:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```

### Ejemplo: comandos en la computadora del profesor (macOS)

Con el repositorio en `~/Documents/Claude/eif509-demo-sesion11`, los
comandos que cambian respecto a esta sección quedan así, listos para copiar. Los
demás pasos son idénticos.

Paso 1, en la terminal 1:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home && cd ~/Documents/Claude/eif509-demo-sesion11 && docker compose down -v && docker compose up -d && git switch inicio && ./gradlew bootRun
```

Paso 1, en la terminal 2:

```bash
cd ~/Documents/Claude/eif509-demo-sesion11 && curl -i http://localhost:8080/api/v1/productos
```

Paso 2, en la terminal 1 (después de `Ctrl+C`):

```bash
git switch main && export JWT_SECRETO=$(openssl rand -base64 48) && ./gradlew bootRun
```

Paso 9, en la terminal 2:

```bash
cd ~/Documents/Claude/eif509-demo-sesion11/spa && npm install && npm run dev
```

Al terminar:

```bash
cd ~/Documents/Claude/eif509-demo-sesion11 && docker compose down -v
```

### Demostración 1 · JWT y roles

**1. La API sin seguridad (paso 5).** En la terminal 1:

```bash
docker compose down -v && docker compose up -d && git switch inicio && ./gradlew bootRun
```

En la terminal 2:

```bash
curl -i http://localhost:8080/api/v1/productos
```

Responde 200 sin credenciales.

**2. Activar la seguridad (pasos 6 y 7).** En la terminal 1, presionen
`Ctrl+C` y ejecuten:

```bash
git switch main && export JWT_SECRETO=$(openssl rand -base64 48) && ./gradlew bootRun
```

**3. Sin token, 401 (paso 8).** En la terminal 2:

```bash
curl -i http://localhost:8080/api/v1/productos
```

**4. Iniciar sesión (paso 9).** Copien el token de la respuesta y péguenlo
en jwt.io:

```bash
curl -s -X POST http://localhost:8080/auth/login -H 'Content-Type: application/json' -d '{"correo": "admin@demo.cr", "clave": "admin123"}'
```

Guarden el token en una variable para los pasos siguientes:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login -H 'Content-Type: application/json' -d '{"correo": "admin@demo.cr", "clave": "admin123"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')
```

**5. Con token, 200 (paso 10):**

```bash
curl -s -o /dev/null -w "%{http_code}\n" -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/productos
```

**6. Rol CLIENTE, 403 (paso 11):**

```bash
TOKEN_CLIENTE=$(curl -s -X POST http://localhost:8080/auth/login -H 'Content-Type: application/json' -d '{"correo": "cliente@demo.cr", "clave": "cliente123"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')
```

```bash
curl -i -X POST http://localhost:8080/api/v1/productos -H "Authorization: Bearer $TOKEN_CLIENTE" -H 'Content-Type: application/json' -d '{"nombre": "Producto del cliente", "precio": 1, "disponible": 1}'
```

**7. Propiedad del recurso (paso 12).** El pedido 1 es de la clienta y
responde 200:

```bash
curl -s -o /dev/null -w "pedido 1 -> %{http_code}\n" -H "Authorization: Bearer $TOKEN_CLIENTE" http://localhost:8080/api/v1/pedidos/1
```

El pedido 3 es de otro cliente y responde 403:

```bash
curl -s -H "Authorization: Bearer $TOKEN_CLIENTE" http://localhost:8080/api/v1/pedidos/3
```

**8. Las pruebas de seguridad (paso 13).** No necesitan Docker ni detener
la API:

```bash
./gradlew unitTest
```

### Demostración 2 · SPA en React

**9. Arrancar la SPA (paso 15).** En la terminal 2:

```bash
cd spa && npm install && npm run dev
```

**10. Usar la SPA (pasos 16 y 17).** Abran `http://localhost:5173` en el
navegador. Entren como `cliente@demo.cr` / `cliente123`, recorran las
páginas e intenten crear un producto: aparece el 403. Después cierren
sesión, entren como `admin@demo.cr` / `admin123` y creen un producto.

**11. El error de CORS (paso 18).** En la terminal 1, presionen `Ctrl+C` y
ejecuten:

```bash
./gradlew bootRun --args='--spring.profiles.active=sin-cors'
```

Recarguen la SPA y revisen el error en la consola del navegador (`F12`,
pestaña *Console*).

**12. Corregir CORS.** En la terminal 1, presionen `Ctrl+C` y ejecuten:

```bash
./gradlew bootRun
```

Recarguen la SPA: vuelve a funcionar.

**13. La API detenida (paso 19).** En la terminal 1, presionen `Ctrl+C` y
recarguen la SPA: aparece el mensaje de error.

### Al terminar

Detengan la SPA con `Ctrl+C` en la terminal 2 y, desde la carpeta del
repositorio, apaguen la base de datos:

```bash
docker compose down -v
```

Dos aspectos que deben tener presentes:

- **Arranquen siempre la API en la terminal 1**, donde definieron
  `JWT_SECRETO`. Si el secreto cambia, la API rechaza los tokens anteriores
  y la SPA vuelve al inicio de sesión.
- **Si algo falla**, la rama `main` contiene la versión completa y las
  secciones siguientes explican cada paso en detalle.

## Estructura del proyecto

Lo nuevo respecto a la Sesión 10 está marcado con `←`:

```text
├── build.gradle                                   ← spring-boot-starter-oauth2-resource-server
├── peticiones/seguridad.http                      ← peticiones de la Demostración 1
├── spa/                                           ← la SPA en React (Vite)
│   └── src/
│       ├── api.js                                 ← un solo lugar para hablar con la API
│       ├── sesion.js                              ← dónde se guarda el token
│       ├── Login.jsx                              ← inicio de sesión
│       ├── ProductoLista.jsx                      ← listado paginado: cargando, datos y error
│       ├── ProductoNuevo.jsx                      ← crear producto (muestra el 403 del CLIENTE)
│       └── App.jsx                                ← sin token: inicio de sesión; con token: catálogo
└── src/
    ├── main/resources/
    │   ├── application.properties                 ← jwt.secreto=${JWT_SECRETO}, cors.origenes
    │   ├── application-sin-cors.properties        ← perfil para mostrar el error de CORS
    │   └── db/migration/V7__usuarios.sql          ← usuarios con rol y clave BCrypt
    ├── main/java/cr/una/eif509/demo/
    │   ├── config/SeguridadConfig.java            ← paso 1: clave, filtro JWT, roles y CORS
    │   ├── config/RespuestasDeSeguridad.java      ← 401 y 403 en formato Problem Details
    │   ├── seguridad/TokenService.java            ← paso 2: emisión del token
    │   ├── api/AuthController.java                ← POST /auth/login
    │   ├── service/PedidoService.java             ← paso 3: verificación de propiedad
    │   └── seguridad/UsuarioActual.java           ← el usuario del token, para el servicio
    └── test/java/cr/una/eif509/demo/
        ├── api/SeguridadApiTest.java              ← paso 4: pruebas de 401, 403 y 200 (sin base)
        └── seguridad/AutenticacionIT.java         ← tokens reales contra PostgreSQL, y CORS
```

## Instalación y configuración

### 1. Clonar el repositorio

```bash
git clone https://github.com/adriangarro/eif509-demo-sesion11.git
cd eif509-demo-sesion11
```

### 2. Verificar que Docker Desktop está en ejecución

```bash
docker info
```

Si ven `Cannot connect to the Docker daemon`, abran Docker Desktop y
esperen a que termine de iniciar.

### 3. Levantar PostgreSQL

```bash
docker compose up -d
docker ps
```

Deben ver `eif509-demo-sesion11-db-1` en estado `Up`. Si aparece
`port is already allocated`, otra base del curso ocupa el puerto 5432:
deténganla desde su carpeta (por ejemplo,
`cd ../eif509-demo-sesion10 && docker compose stop`) y reintenten.

### 4. Verificar Java y Node.js

```bash
java -version
node --version
```

Java debe reportar 21 o superior y Node.js, 20 o superior.

## Demostración 1 · Proteger la API con JWT y roles

### 5. (Opcional) La API sin seguridad: rama `inicio`

```bash
git switch inicio
./gradlew bootRun
```

Desde una **segunda terminal** en la misma carpeta:

```bash
curl -i http://localhost:8080/api/v1/productos
```

Salida esperada: `HTTP/1.1 200` y la lista de productos, sin credenciales.
Un `POST` también funciona: cualquiera puede crear productos. Así está hoy
la API de la mayoría de los proyectos.

Detengan la aplicación con `Ctrl+C` en la primera terminal y vuelvan a la
versión completa:

```bash
git switch main
```

### 6. Definir el secreto JWT

La API firma y verifica los tokens con una clave secreta (HS256). La clave
se lee de la variable de entorno `JWT_SECRETO`, nunca se escribe en el
código ni se sube al repositorio, y debe tener **al menos 32 caracteres**.
En la terminal donde van a arrancar la API:

```bash
export JWT_SECRETO=$(openssl rand -base64 48)
```

En Windows (PowerShell), definan cualquier texto de 32 caracteres o más:

```powershell
$env:JWT_SECRETO = "una-clave-larga-de-al-menos-32-caracteres-para-hs256"
```

### 7. Arrancar la API

```bash
./gradlew bootRun
```

Salida esperada al final del arranque:

```text
o.f.core.internal.command.DbMigrate      : Successfully applied 7 migrations to schema "public", now at version v7
cr.una.eif509.demo.DemoApplication       : Started DemoApplication in 1.57 seconds
```

Si antes hicieron el paso 5, verán `Successfully applied 1 migration`:
Flyway aplica solo la V7. Si la API ya arrancó antes con esta base, verán
`Schema "public" is up to date`: no hay migraciones pendientes.

Si falta la variable, la aplicación no arranca y el mensaje lo indica:
`Could not resolve placeholder 'JWT_SECRETO'`. Si es demasiado corta:
`JWT_SECRETO debe tener al menos 32 caracteres (tiene 5). HS256 requiere una clave de 256 bits.`

Los comandos de los pasos siguientes van en la **segunda terminal**.

### 8. Sin token: 401

```bash
curl -i http://localhost:8080/api/v1/productos
```

```text
HTTP/1.1 401
WWW-Authenticate: Bearer

{"type":"https://api.ejemplo.cr/errores/no-autenticado","title":"No autenticado","status":401,"detail":"Se requiere un token válido en la cabecera Authorization.","instance":"/api/v1/productos"}
```

**Qué observar**: la misma petición que en el paso 5 respondía 200. El
filtro de seguridad no encontró un token válido y respondió antes de llegar
al controlador. La configuración está en
[`SeguridadConfig.java`](src/main/java/cr/una/eif509/demo/config/SeguridadConfig.java):

```java
.securityMatcher("/api/**", "/auth/**")
.csrf(csrf -> csrf.disable())
.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
.authorizeHttpRequests(a -> a
        .requestMatchers("/auth/login").permitAll()
        .requestMatchers(HttpMethod.POST, "/api/v1/productos/**").hasRole("ADMIN")
        .anyRequest().authenticated())
.oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(convertidorDeRoles())))
```

`STATELESS` indica que el servidor no crea sesiones; por eso CSRF se
deshabilita en la API (Sesión 10). Solo `/auth/login` es público.

### 9. Iniciar sesión: el token

```bash
curl -s -X POST http://localhost:8080/auth/login -H 'Content-Type: application/json' -d '{"correo": "admin@demo.cr", "clave": "admin123"}'
```

```text
{"token":"eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJlaWY1MDkiLCJzdWIiOiJhZG1pbkBkZW1vLmNyIiwiZXhw...","tipo":"Bearer","expiraEnSegundos":3600}
```

Copien el valor de `token` y péguenlo en [jwt.io](https://jwt.io). El
contenido (payload) es:

```json
{"iss":"eif509","sub":"admin@demo.cr","exp":1790881329,"iat":1790877729,"roles":["ADMIN"]}
```

**Qué observar**: `sub` es quién es, `roles` qué puede hacer y `exp` hasta
cuándo vale (una hora). Cualquiera puede leer el contenido; lo que no puede
hacer es modificarlo sin invalidar la firma. Por eso un token nunca lleva
datos sensibles. Lo emite
[`TokenService.java`](src/main/java/cr/una/eif509/demo/seguridad/TokenService.java)
y lo devuelve
[`AuthController.java`](src/main/java/cr/una/eif509/demo/api/AuthController.java)
después de comparar la clave con el hash BCrypt.

Para los pasos siguientes, guarden el token en una variable:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login -H 'Content-Type: application/json' -d '{"correo": "admin@demo.cr", "clave": "admin123"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')
```

### 10. Con el token: 200

```bash
curl -s -o /dev/null -w "%{http_code}\n" -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/productos
```

Salida esperada: `200`. El servidor no guardó nada al iniciar sesión:
verificó la firma y la expiración del token en esta misma petición.

Como `ADMIN`, también puede crear productos:

```bash
curl -i -X POST http://localhost:8080/api/v1/productos -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"nombre": "Café Tarrazú 1 kg", "precio": 9800.00, "disponible": 25}'
```

Salida esperada: `HTTP/1.1 201` con `Location: /api/v1/productos/13`.

### 11. Rol sin permiso: 403

Inicien sesión como `cliente@demo.cr` e intenten crear un producto:

```bash
TOKEN_CLIENTE=$(curl -s -X POST http://localhost:8080/auth/login -H 'Content-Type: application/json' -d '{"correo": "cliente@demo.cr", "clave": "cliente123"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')
curl -i -X POST http://localhost:8080/api/v1/productos -H "Authorization: Bearer $TOKEN_CLIENTE" -H 'Content-Type: application/json' -d '{"nombre": "Producto del cliente", "precio": 1, "disponible": 1}'
```

```text
HTTP/1.1 403

{"type":"https://api.ejemplo.cr/errores/acceso-denegado","title":"Acceso denegado","status":403,"detail":"Su rol no tiene permiso para realizar esta operación.","instance":"/api/v1/productos"}
```

**Qué observar**: el usuario está **autenticado** (el token es válido), pero
no está **autorizado**: su rol no le permite crear productos. Esa es la
diferencia entre autenticación (¿quién es?) y autorización (¿puede hacer
esto?).

El convertidor `convertidorDeRoles()` transforma la claim `roles` del token
en las autoridades que usa Spring (`ROLE_ADMIN`, `ROLE_CLIENTE`); sin él,
`hasRole("ADMIN")` siempre respondería 403.

### 12. Propiedad del recurso: 403

Con el token de la clienta, el pedido 1 es suyo y el 3 es de otro cliente:

```bash
curl -s -o /dev/null -w "pedido 1 -> %{http_code}\n" -H "Authorization: Bearer $TOKEN_CLIENTE" http://localhost:8080/api/v1/pedidos/1
curl -s -H "Authorization: Bearer $TOKEN_CLIENTE" http://localhost:8080/api/v1/pedidos/3
```

```text
pedido 1 -> 200
{"type":"https://api.ejemplo.cr/errores/acceso-denegado","title":"Acceso denegado","status":403,"detail":"No tiene permiso para consultar este recurso","instance":"/api/v1/pedidos/3"}
```

**Qué observar**: un token válido no autoriza a ver cualquier pedido. Esta
regla depende de los datos, por eso no se puede declarar en la
configuración y vive en el servicio
([`PedidoService.java`](src/main/java/cr/una/eif509/demo/service/PedidoService.java)):

```java
public PedidoResumen obtener(Long pedidoId, UsuarioActual usuario) {
    var pedido = pedidos.findById(pedidoId)
            .orElseThrow(() -> new PedidoNoExisteException(pedidoId));
    if (!usuario.esAdmin() && !pedido.getCliente().getEmail().equals(usuario.correo())) {
        throw new AccesoDenegadoException();   // -> 403
    }
    return PedidoMapper.aResumen(pedido);
}
```

El controlador construye `UsuarioActual` a partir del token
(`auth.getName()` es el `sub`). Es el riesgo número 1 de OWASP para API
(API1: *Broken Object Level Authorization*). El listado
`GET /api/v1/pedidos` aplica la misma regla: un cliente recibe solo sus
pedidos.

Un inicio de sesión fallido responde con el mismo mensaje si el correo no
existe o si la clave es incorrecta, para no revelar qué correos están
registrados:

```bash
curl -s -X POST http://localhost:8080/auth/login -H 'Content-Type: application/json' -d '{"correo": "admin@demo.cr", "clave": "incorrecta"}'
```

```text
{"type":"https://api.ejemplo.cr/errores/credenciales-invalidas","title":"Credenciales inválidas","status":401,"detail":"Correo o clave incorrectos","instance":"/auth/login"}
```

### 13. Las pruebas de seguridad

Sin Docker:

```bash
./gradlew unitTest
```

[`SeguridadApiTest`](src/test/java/cr/una/eif509/demo/api/SeguridadApiTest.java)
contiene las pruebas de la lámina 9: sin token, 401; con el rol `CLIENTE`,
403; con el rol `ADMIN`, 201. `jwt()` de `spring-security-test` simula una
petición autenticada con las autoridades indicadas, sin generar tokens
reales:

```java
mvc.perform(post("/api/v1/productos")
        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE")))
        .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_JSON))
   .andExpect(status().isForbidden());
```

Con Docker, todas las pruebas (73):

```bash
./gradlew test
```

[`AutenticacionIT`](src/test/java/cr/una/eif509/demo/seguridad/AutenticacionIT.java)
repite el recorrido de esta demostración con tokens reales contra PostgreSQL
(Testcontainers): inicio de sesión correcto e incorrecto, 401, 200, 403 por
rol, 403 por propiedad del recurso y CORS. Las pruebas usan un secreto JWT
propio, definido en `build.gradle`; no necesitan la variable de entorno.

### 14. (Adicional) Swagger UI con el token

Abran `http://localhost:8080/swagger-ui.html`. Ejecuten `POST /auth/login`,
copien el token, pulsen **Authorize** y péguenlo. A partir de ahí, Swagger
UI envía la cabecera `Authorization: Bearer <token>` en cada petición.

## Demostración 2 · Una SPA en React que consume la API

La API debe seguir en ejecución (paso 7).

### 15. Arrancar la SPA

En la segunda terminal:

```bash
cd spa
npm install
npm run dev
```

Salida esperada:

```text
VITE v8.3.2  ready in 190 ms
➜  Local:   http://localhost:5173/
```

El proyecto se generó con `npm create vite@latest spa -- --template react`,
el mismo comando de la clase; luego se reemplazaron los archivos de
ejemplo por los de este catálogo.

### 16. Iniciar sesión desde la SPA

Abran `http://localhost:5173` e inicien sesión con `cliente@demo.cr` /
`cliente123`.

Salida esperada: la barra superior muestra `cliente@demo.cr (CLIENTE)`, el
texto **«Productos en el catálogo: 13»** (o 12 si no crearon el producto
del paso 10) y una tabla de 5 productos con los botones **« Anterior** y
**Siguiente »**.

Todas las llamadas pasan por [`spa/src/api.js`](spa/src/api.js):

```javascript
export async function api(ruta, opciones = {}) {
  const token = leerToken();
  resp = await fetch(API_URL + ruta, {
    ...opciones,
    headers: {
      "Content-Type": "application/json",
      ...(token && { Authorization: `Bearer ${token}` }),
    },
  });
  if (resp.status === 401 && token) {          // token vencido o inválido
    borrarToken();
    window.location.href = "/login";
  }
  if (!resp.ok) throw await leerProblema(resp); // Problem Details
  return resp.json();
}
```

**Qué observar**: la función agrega el token y, si la API responde 401,
regresa al inicio de sesión. Así no se repite esta lógica en cada pantalla.

### 17. Los tres estados de una consulta

[`spa/src/ProductoLista.jsx`](spa/src/ProductoLista.jsx) consulta
`GET /api/v1/productos?page=0&size=5` en un efecto (`useEffect`) y muestra
uno de tres estados: **«Cargando...»**, la tabla con los datos, o el
mensaje de error. Pulsen **Siguiente »**: la consulta se repite con
`page=1`.

Con el rol `CLIENTE`, intenten crear un producto en el formulario
**Nuevo producto**. Salida esperada, en rojo:

```text
Acceso denegado: Su rol no tiene permiso para realizar esta operación.
```

El mensaje es el Problem Details que la API devuelve desde la Sesión 9.
Cierren sesión, entren como `admin@demo.cr` / `admin123` y repitan: el
producto se crea, aparece el mensaje verde y la lista se recarga.

### 18. El error de CORS

Detengan la API (`Ctrl+C` en la primera terminal) y arránquenla con el
perfil que no autoriza ningún origen:

```bash
./gradlew bootRun --args='--spring.profiles.active=sin-cors'
```

Recarguen la SPA. Salida esperada en la pantalla:

```text
No se pudo conectar con la API. Verifiquen que esté en ejecución y que permita este origen (CORS).
```

Y en la consola del navegador (`F12` → *Console*):

```text
Access to fetch at 'http://localhost:8080/api/v1/productos?page=0&size=5' from origin 'http://localhost:5173'
has been blocked by CORS policy: Response to preflight request doesn't pass access control check:
No 'Access-Control-Allow-Origin' header is present on the requested resource.
```

**Qué observar**: con `curl` la misma petición funciona; el navegador no.
La SPA está en el puerto 5173 y la API en el 8080: son orígenes distintos,
y el navegador solo entrega la respuesta si el servidor autoriza ese origen
de forma explícita. Antes de una petición con la cabecera `Authorization`,
el navegador envía una petición previa (`OPTIONS`) para consultar el
permiso; aquí esa consulta recibe 403.

Detengan la API y arránquenla sin el perfil:

```bash
./gradlew bootRun
```

Recarguen la SPA: ahora funciona. Arranquen siempre la API en la misma
terminal donde definieron `JWT_SECRETO`: si el secreto cambia, la API
rechaza los tokens emitidos con el anterior (401) y la SPA vuelve al
inicio de sesión, que es el comportamiento esperado.

La configuración está en
`SeguridadConfig.corsConfigurationSource()`:

```java
config.setAllowedOrigins(List.of("http://localhost:5173"));   // de cors.origenes
config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
```

Se autoriza un origen específico, nunca `*`. En producción se indica el
dominio real del frontend con la variable `CORS_ORIGENES`. CORS no protege
al servidor: solo controla qué respuestas pueden leer los navegadores.

### 19. La API detenida

Detengan la API con `Ctrl+C` y recarguen la SPA. Salida esperada: el mismo
mensaje rojo del paso 18. La pantalla muestra el error en lugar de quedarse
en blanco o en «Cargando...».

## Dónde guardar el token en el cliente

Esta SPA lo guarda en `localStorage` ([`spa/src/sesion.js`](spa/src/sesion.js)).

| Opción | Ventaja | Riesgo |
|---|---|---|
| `localStorage` (esta SPA) | Sencilla; el token sobrevive a recargas de la página | Cualquier script de la página puede leerlo: un XSS permite robarlo |
| Memoria (variable de estado) | No queda guardado al cerrar la pestaña | Se pierde al recargar; exige iniciar sesión otra vez |
| Cookie `httpOnly` | JavaScript no puede leerla | El navegador la envía sola: requiere protección CSRF |

Para el curso, `localStorage` es aceptable porque React escapa el contenido
por defecto y el token vence en una hora. La decisión y su justificación se
documentan en un ADR.

Con JWT, «cerrar sesión» es descartar el token en el cliente: el token
sigue siendo válido hasta que expire, por eso su vigencia es corta.

## Comandos útiles

| Acción | Comando |
|---|---|
| Levantar la base | `docker compose up -d` |
| Definir el secreto (macOS y Linux) | `export JWT_SECRETO=$(openssl rand -base64 48)` |
| Arrancar la API | `./gradlew bootRun` |
| Arrancar la API sin CORS (demostración del error) | `./gradlew bootRun --args='--spring.profiles.active=sin-cors'` |
| Arrancar la SPA | `cd spa && npm install && npm run dev` |
| SPA | `http://localhost:5173` |
| Módulo administrativo (Sesión 10) | `http://localhost:8080/admin/productos` (`admin@demo.cr` / `admin123`) |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Solo pruebas unitarias (sin Docker) | `./gradlew unitTest` |
| Todas las pruebas y cobertura | `./gradlew test` |
| Revisar y compilar la SPA (lo que ejecuta el CI) | `cd spa && npm run lint && npm run build` |
| Reiniciar la demo desde cero | `docker compose down -v && docker compose up -d` |

## Solución de problemas

| Problema | Causa | Solución |
|---|---|---|
| `Could not resolve placeholder 'JWT_SECRETO'` al arrancar | Falta la variable de entorno | Definirla en la misma terminal (paso 6) |
| `JWT_SECRETO debe tener al menos 32 caracteres` | La clave es demasiado corta para HS256 | Usar 32 caracteres o más; `openssl rand -base64 48` genera una adecuada |
| 401 incluso con el token | La cabecera no es exactamente `Authorization: Bearer <token>`, el token venció, o la API se reinició con otro `JWT_SECRETO` | Revisar la cabecera; iniciar sesión otra vez para obtener un token nuevo |
| `hasRole("ADMIN")` siempre responde 403 | Falta el convertidor de roles o el prefijo `ROLE_` | Ver `convertidorDeRoles()` en `SeguridadConfig`; comparar con el contenido del token en jwt.io |
| La SPA muestra «No se pudo conectar con la API» | La API está detenida, o se arrancó con el perfil `sin-cors` | Arrancar la API sin el perfil (paso 7) |
| CORS sigue fallando después de configurarlo | El origen no coincide exactamente (protocolo y puerto) | El origen debe ser `http://localhost:5173`; si la SPA usa otro puerto, definir `CORS_ORIGENES` antes de arrancar la API |
| `npm: command not found` | Falta Node.js | Instalarlo desde nodejs.org |
| `Port 5173 is in use` | Otra instancia de Vite en ejecución | Detenerla con `Ctrl+C`, o usar el puerto que Vite propone y definir `CORS_ORIGENES` con ese puerto |
| `Port 8080 was already in use` | Otra aplicación en ejecución (por ejemplo, la de la Sesión 10) | Detenerla con `Ctrl+C` |
| `Bind for 0.0.0.0:5432 failed: port is already allocated` | Otra base del curso usa el puerto | `docker ps` para ver cuál y `docker compose stop` desde su carpeta |
| El módulo administrativo no acepta `admin` / `admin123` | Desde esta sesión, el usuario es un correo | Usar `admin@demo.cr` / `admin123` |
| `Unable to locate a Java Runtime` | Falta el JDK o `JAVA_HOME` | Ver Requisitos previos |

## Relación con los laboratorios

**Laboratorio 5** (entrega el jueves 8 de octubre a las 6:00 p. m.): este
repositorio cubre el criterio de seguridad (2 puntos): `POST /auth/login`
que emite el token, API sin estado, dos roles con autorización por
endpoint, verificación de propiedad del recurso en el servicio, y las
pruebas de 401, 403 y 200. En el taller de hoy apliquen los pasos 1 a 4
(configuración, emisión del token, roles y propiedad, pruebas) a su propio
proyecto.

**Laboratorio 6** (se asigna el jueves 8): la SPA de su dominio sobre su
API. Este repositorio muestra la base: el módulo cliente con el token, la
pantalla de inicio de sesión, un listado con sus tres estados y la
configuración de CORS.

---

> **Material de referencia del curso.** La seguridad y las pantallas de su
> laboratorio deben diseñarse a partir de su propio dominio; no copien este
> ejemplo.
