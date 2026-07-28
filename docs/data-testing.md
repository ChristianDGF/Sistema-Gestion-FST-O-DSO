# Guía de Pruebas de Datos (Data Testing)

Este documento cubre la sección **Data Testing** de la consigna del Proyecto Final
(Migraciones, Integridad de datos, Datos duplicados, Constraints y Seeds), qué se
implementó, cómo ejecutarlo y qué se encontró en el proceso.

Toda la suite vive en
[`src/test/java/proyecto/sistemaGestion/data/`](../src/test/java/proyecto/sistemaGestion/data/)
y comparte una base común,
[`AbstractDataTest`](../src/test/java/proyecto/sistemaGestion/data/AbstractDataTest.java),
que la diferencia del resto de las pruebas de integración del proyecto: `ProductIntegrationTest`
y el perfil `test` (`application-test.properties`) deshabilitan Flyway y usan
`spring.jpa.hibernate.ddl-auto=update`, es decir, generan el esquema a partir de las
entidades JPA. `AbstractDataTest` hace lo contrario a propósito — deja
`spring.flyway.enabled=true` y usa `ddl-auto=validate` — para que el esquema bajo
prueba sea el que realmente construyen los scripts de
[`db/migration`](../src/main/resources/db/migration/), no uno inferido por Hibernate.
El contenedor de Postgres (Testcontainers) se levanta una sola vez y el contexto de
Spring se reutiliza entre las cinco clases de test.

## 1. Migraciones

[`FlywayMigrationTest`](../src/test/java/proyecto/sistemaGestion/data/FlywayMigrationTest.java)
valida tres cosas:

- El contexto de Spring arranca con `ddl-auto=validate` — si una migración quedara
  desincronizada de las entidades `@Entity`, todo el arranque fallaría aquí, no en
  producción.
- No quedan migraciones pendientes tras el arranque (`flyway.info().pending()` vacío)
  y se aplicaron al menos `V1`…`V5`.
- Volver a llamar `flyway.migrate()` sobre un esquema ya actualizado es idempotente
  (`migrationsExecuted == 0`).

### Hallazgo

Antes de este trabajo, **ninguna prueba automatizada ejecutaba los scripts de Flyway
reales**: tanto la suite de integración existente como el perfil `test` usan
`ddl-auto=update`/Flyway deshabilitado. Las migraciones `V1`-`V3` solo se habían
probado manualmente contra el Postgres de `docker-compose`. `FlywayMigrationTest`
cierra ese hueco sin tocar la suite existente (usa su propio paquete y configuración).

```bash
./gradlew test --tests "proyecto.sistemaGestion.data.FlywayMigrationTest"
```

## 2. Constraints

Los invariantes de negocio (`@Positive`, `@PositiveOrZero`, `@NotBlank` en
`Product`/`StockMovement`) solo existían como Bean Validation, es decir, solo se
respetan si el dato pasa por `ProductService`/`StockMovementService`. La migración
[`V4__add_data_constraints.sql`](../src/main/resources/db/migration/V4__add_data_constraints.sql)
sube esos mismos invariantes al esquema con `CHECK` constraints: precio positivo,
cantidad y `min_stock` no negativos, `sku`/`name`/`user_id` no vacíos, y
`status`/`movement_type` restringidos a los valores válidos del enum.

[`SchemaConstraintsTest`](../src/test/java/proyecto/sistemaGestion/data/SchemaConstraintsTest.java)
inserta filas **vía JDBC crudo** (`JdbcTemplate`), no a través de los servicios,
precisamente para probar que la base de datos por sí sola —sin pasar por Bean
Validation— rechaza el dato inválido. Cubre: precio negativo/cero, cantidad negativa,
`min_stock` negativo, `sku`/`name` en blanco, `status` inválido, `movement_type`
inválido, cantidad de movimiento no positiva, y `product_id` inexistente (FK).

```bash
./gradlew test --tests "proyecto.sistemaGestion.data.SchemaConstraintsTest"
```

## 3. Integridad de datos

[`DataIntegrityTest`](../src/test/java/proyecto/sistemaGestion/data/DataIntegrityTest.java)
prueba el caso donde se borra un producto que tiene historial de movimientos
asociado (`stock_movements.product_id` referencia `products.id` sin `ON DELETE`
explícito, por lo que Postgres lo rechaza por defecto).

### Hallazgo

Al auditar `ProductService.delete()` se esperaba encontrar un gap (una
`DataIntegrityViolationException` sin traducir escapando al cliente), pero el código
ya contempla el caso correctamente: verifica
`stockMovementRepository.existsByProductId(id)` antes de borrar y lanza una
`BusinessException` controlada. `DataIntegrityTest` documenta y fija ese
comportamiento como regresión, y además prueba el escenario en el que alguien se
salta esa validación de servicio (borrado directo vía JDBC) — ahí la
`FOREIGN KEY` de la base de datos sigue bloqueando el borrado como defensa en
profundidad. También se confirma que un producto sin movimientos se borra sin
problema.

```bash
./gradlew test --tests "proyecto.sistemaGestion.data.DataIntegrityTest"
```

## 4. Datos duplicados

`ProductIntegrationTest.shouldThrowException_whenCreatingDuplicateSku` ya cubre el
duplicado a través de `ProductService` (que hace `existsBySku` antes de insertar).
[`DuplicateDataTest`](../src/test/java/proyecto/sistemaGestion/data/DuplicateDataTest.java)
cubre lo que ese chequeo previo no puede cubrir:

- Un `INSERT` directo con un `sku` repetido, saltándose la capa de servicio —
  la única defensa real ahí es el `UNIQUE(sku)` de la tabla.
- Una condición de carrera real: dos hilos insertando el mismo `sku`
  concurrentemente (sincronizados con un `CountDownLatch` para maximizar el
  solape); se verifica que exactamente uno tiene éxito y el otro falla por el
  `UNIQUE` constraint.

```bash
./gradlew test --tests "proyecto.sistemaGestion.data.DuplicateDataTest"
```

## 5. Seeds

No existía ningún dato de referencia en el esquema (solo `V1`-`V3`). Se agregó
[`V5__seed_reference_data.sql`](../src/main/resources/db/migration/V5__seed_reference_data.sql)
con tres productos demo, usando `ON CONFLICT (sku) DO NOTHING` para que la
migración sea segura de re-ejecutar.

[`SeedDataTest`](../src/test/java/proyecto/sistemaGestion/data/SeedDataTest.java)
valida que los tres productos semilla existen tras las migraciones, que
satisfacen los mismos `CHECK` constraints que cualquier dato real (`V4`), y que
volver a ejecutar el mismo `INSERT ... ON CONFLICT DO NOTHING` no duplica filas.

```bash
./gradlew test --tests "proyecto.sistemaGestion.data.SeedDataTest"
```

## 6. Resultado de la última corrida

`./gradlew test` completo (suite existente + los 21 tests nuevos de `data.*`):
**BUILD SUCCESSFUL**, sin regresiones en el resto de la suite (unit, contract,
integración, seguridad).

## 7. Integración en el pipeline

No se necesitó un stage/job nuevo: estas pruebas requieren Docker (Testcontainers)
igual que `ProductIntegrationTest`, así que corren dentro del mismo paso que ya
ejecuta el resto de `./gradlew test`:

- **GitHub Actions** (`.github/workflows/ci.yml`): step `Run Build and Tests (Unit,
  Contract, Integration & Data)` del job `build-and-test`.
- **Jenkins** (`Jenkinsfile`): stage `Run Unit, Contract & Data Tests`.

Data Testing no requiere un ambiente Preview/Staging desplegado (a diferencia de
Integration/API/E2E/Security) — por naturaleza valida el esquema y los datos, algo
que se verifica igual de bien contra un Testcontainer efímero en cada corrida de CI.
