# Microservicio de Autenticación – CrediYa

Este proyecto implementa un microservicio de autenticación/registro de usuarios basado en Clean Architecture, usando Java 17 y Spring Boot (WebFlux + R2DBC) con PostgreSQL.

## Arquitectura y módulos

Clean Architecture separa el dominio del detalle de infraestructura. Los módulos Gradle principales son:

- model (domain/model):
  - Entidades de dominio (por ejemplo, `Usuario`).
  - Puertos/gateways (por ejemplo, `UsuarioRepository`).
- usecase (domain/usecase):
  - Casos de uso de aplicación (por ejemplo, `RegistrarUsuarioUseCase`).
- reactive-web (infrastructure/entry-points/reactive-web):
  - Entry point HTTP con Spring WebFlux (Router/Handler).
  - DTOs y mapeo con MapStruct.
  - Filtros de CORS y cabeceras de seguridad.
- r2dbc-postgresql (infrastructure/driven-adapters/r2dbc-postgresql):
  - Adaptador de persistencia reactiva (R2DBC) a PostgreSQL.
  - Entidad de datos `UsuarioEntity`, repositorio reactivo y configuración de pool.
- app-service (applications/app-service):
  - Aplicación Spring Boot que ensambla y levanta el servicio.

## Stack técnico

- Java 17, Gradle 8.x (wrapper incluido)
- Spring Boot 3.5.4 (WebFlux, Actuator)
- R2DBC PostgreSQL, Connection Pool
- Reactor (Mono/Flux)
- Validación: Jakarta Validation + Hibernate Validator
- DTO mapping: MapStruct
- Observabilidad: Micrometer Prometheus
- Calidad: JUnit 5, Jacoco, PIT Mutation Testing

## Requisitos

- JDK 17
- PostgreSQL 12+
- (Opcional) Docker 24+

## Configuración

La configuración principal está en `applications/app-service/src/main/resources/application.yaml`:

```yaml
server:
  port: 8080
spring:
  application:
    name: "CrediYa"
  devtools:
    add-properties: false
  h2:
    console:
      enabled: true
      path: "/h2"
management:
  endpoints:
    web:
      exposure:
        include: "health,prometheus"
  endpoint:
    health:
      probes:
        enabled: true
adapters:
  r2dbc:
    host: "localhost"
    port: 5432
    database: "CrediYa-Autenticacion"
    schema: "public"
    username: ${DATABASE_USERNAME_CrediYa-Autenticacion:"USERNAME"}
    password: ${DATABASE_PASSWORD_CrediYa-Autenticacion:"PASSWORD"}
cors:
  allowed-origins: "http://localhost:4200,http://localhost:8080"
logging:
  level:
    root: INFO
  file:
    name: logs/crediya.log
```

- Conexión R2DBC: se mapea a `PostgresqlConnectionProperties` con prefijo `adapters.r2dbc`.
- CORS: configurable mediante `cors.allowed-origins`.
- Actuator: expone `/actuator/health` y `/actuator/prometheus`.

## Compilar y ejecutar

- Compilar todo el proyecto:
  - `./gradlew clean build`
- Ejecutar la app localmente:
  - `./gradlew :app-service:bootRun`
- Empaquetar JAR ejecutable:
  - `./gradlew :app-service:bootJar`
  - JAR resultante: `build/libs/CrediYa.jar`
- Ejecutar el JAR:
  - `java -jar build/libs/CrediYa.jar`

### Docker (opcional)

Un Dockerfile está disponible en `deployment/Dockerfile`.

Ejemplo:

```bash
docker build -f deployment/Dockerfile -t crediya-auth .
docker run --rm -p 8080:8080 \
  -e DATABASE_USERNAME_CrediYa-Autenticacion=USERNAME \
  -e DATABASE_PASSWORD_CrediYa-Autenticacion=PASSWORD \
  crediya-auth
```

Nota: Ajusta host/puerto si PostgreSQL corre fuera del contenedor.

## Endpoints

### Registrar usuario

- Método: POST
- URL: `/api/v1/usuarios`
- Content-Type: `application/json`

Body (DTO `UsuarioRequest`):

```json
{
  "nombre": "Juan",
  "apellido": "Pérez",
  "fechaNacimiento": "1990-01-15T00:00:00Z",
  "telefono": "+57-3000000000",
  "email": "juan.perez@example.com",
  "salarioBase": 1200000
}
```

Respuestas:

- 200 OK: `UsuarioResponse` con los datos creados y `userId`.
- 400 Bad Request: `{ "error": "mensaje de error" }` por validación o si el email ya está registrado.

Ejemplo con curl:

```bash
curl -X POST http://localhost:8080/api/v1/usuarios \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Ana",
    "apellido": "Gómez",
    "fechaNacimiento": "1995-05-20T00:00:00Z",
    "telefono": "+57-3111111111",
    "email": "ana.gomez@example.com",
    "salarioBase": 2500000
  }'
```

## Comportamiento clave

- Validaciones (Jakarta Validation) en `UsuarioRequest`:
  - Campos obligatorios, formato de email y rango de `salarioBase`.
- Reglas de negocio (use case `RegistrarUsuarioUseCase`):
  - Verifica que el email no exista antes de guardar.
- Persistencia (R2DBC):
  - `UsuarioRepositoryAdapter` mapea entre modelo de dominio y `UsuarioEntity`.
- Seguridad y CORS:
  - Filtro de cabeceras de seguridad (`SecurityHeadersConfig`).
  - CORS configurable; métodos permitidos GET/POST.

## Pruebas y calidad

- Ejecutar pruebas: `./gradlew test`
- Reporte de cobertura Jacoco consolidado: `./gradlew jacocoMergedReport`
  - Salida: `build/reports/jacocoHtml/index.html` y `build/reports/jacoco.xml`
- Mutación (PIT) agregada: `./gradlew pitestReportAggregate`
  - Reporte: `build/reports/pitest/mutations.xml` y HTML por submódulo

## Estructura (resumen)

```
applications/
  app-service/
    src/main/resources/application.yaml
domain/
  model/ (entidades y gateways)
  usecase/ (casos de uso)
infrastructure/
  entry-points/reactive-web/ (Router, Handler, DTOs, CORS, headers)
  driven-adapters/r2dbc-postgresql/ (config R2DBC, entity, repos, adapter)
```

## Notas

- Java 17 es requerido (configurado en `main.gradle`).
- El repositorio reactivo expone `findByEmail` y el use case impide duplicados por email.
- Ajusta `adapters.r2dbc.*` para apuntar a tu base PostgreSQL.
