# ClienteInventario — DistriSalva, S.A. de C.V.

Proyecto base de **Programación III (PROG3)** · Universidad Tecnológica de El Salvador · Facultad de Informática y Ciencias Aplicadas · Ciclo 02-2026.

Cliente de inventario en Java que guarda productos en una base de datos **H2** mediante **JDBC** y respalda el inventario en **JSON** con **Jackson**. En el **Examen Parcial 2** usted agregará el consumo de una **API REST externa**.

---

## Estado actual del proyecto

| Capa | Clase | Responsabilidad |
|---|---|---|
| `modelo` | `Producto` | Entidad con `id`, `nombre` y `cantidad` |
| `datos` | `ConexionDB` | Abre la conexión JDBC con H2 (`jdbc:h2:./inventario`) |
| `datos` | `ProductoDAO` | CRUD con `PreparedStatement`: crear tabla, insertar, listar, actualizar, eliminar, existe |
| `servicio` | `InventarioJsonService` | Exporta e importa el inventario en `inventario.json` con `ObjectMapper` |
| raíz | `Main` | Coordina el flujo: crea la tabla, siembra datos, respalda, modifica y restaura |
| raíz | `PruebaJackson` | Clase de práctica de la guía N.º 2 (no forma parte del entregable) |

```
src/main/java/sv/edu/utec/
|-- Main.java
|-- PruebaJackson.java
|-- modelo/Producto.java
|-- datos/ConexionDB.java
|-- datos/ProductoDAO.java
|-- servicio/InventarioJsonService.java
```

**Dependencias** (`pom.xml`): `com.h2database:h2:2.2.224` y `com.fasterxml.jackson.core:jackson-databind:2.17.2`.

## Cómo ejecutar

1. Abra la carpeta del proyecto en IntelliJ IDEA (**File › Open**) y pulse **Load Maven Changes**.
2. Verifique que el proyecto usa **JDK 21** (**File › Project Structure › Project SDK**).
3. Ejecute `sv.edu.utec.Main`.

La base de datos `inventario.mv.db` **no** se incluye en el repositorio: se crea sola la primera vez que se ejecuta `Main`. Si quiere empezar de cero, cierre IntelliJ Database (si la tiene conectada) y elimine ese archivo.

---

# Examen Parcial 2 — Ejercicio práctico (60 %)

**Modalidad:** individual · **Entrega:** enlace de **su** repositorio de GitHub en la tarea de Moodle **«Parcial 2 — Enlace del repositorio»**.

## Caso

El proveedor de DistriSalva publica su catálogo en una API REST. Usted debe hacer que el cliente **descargue los productos del proveedor, los convierta a objetos `Producto` y los guarde en la base de datos local sin duplicarlos**.

**Requisitos:** IntelliJ IDEA, **JDK 21**, Git, una cuenta de GitHub y conexión a internet.

## Paso 0 — Preparar su repositorio

Usted no tiene permiso para subir cambios al repositorio del docente. Por eso debe clonarlo y apuntar el remoto `origin` a un repositorio propio.

**0.1** En GitHub, cree un repositorio **público y vacío**, sin README, sin `.gitignore` y sin licencia, con este nombre:

```
ClienteInventario-P2-<carnet>        ejemplo: ClienteInventario-P2-2527012345
```

**0.2** Abra una terminal (puede usar la terminal integrada de IntelliJ) y ejecute:

```bash
# Clonar el repositorio base
git clone https://github.com/iErnesto/ClienteInventario.git ClienteInventario-P2
cd ClienteInventario-P2

# Ver el remoto actual (apunta al docente)
git remote -v

# Cambiar el remoto a SU repositorio
git remote set-url origin https://github.com/<su-usuario>/ClienteInventario-P2-<carnet>.git
git remote -v        # verificar que ahora aparece su usuario

# Subir el estado inicial y crear la rama de trabajo
git push -u origin main
git switch -c feat/consumo-api
```

**0.3** En IntelliJ, abra la carpeta con **File › Open**, pulse **Load Maven Changes** y ejecute `Main` para comprobar que el proyecto base funciona.

> **Importante:** configure su identidad antes del primer commit si aún no lo ha hecho:
> `git config --global user.name "Nombre Apellido"` y `git config --global user.email "su-correo"`.

---

## Material de apoyo — el servicio del proveedor

**Endpoint** (método **GET**):

```
https://dummyjson.com/products?limit=10&select=title,stock
```

**Respuesta** (resumida). Los productos llegan **dentro** de la clave `products`, junto con datos de paginación:

```json
{
  "products": [
    { "id": 1, "title": "Essence Mascara Lash Princess", "stock": 99 },
    { "id": 2, "title": "Eyeshadow Palette with Mirror", "stock": 34 },
    { "id": 3, "title": "Powder Canister", "stock": 89 }
  ],
  "total": 194,
  "skip": 0,
  "limit": 10
}
```

**Correspondencia de campos:**

| JSON del proveedor | Atributo de `Producto` |
|---|---|
| `id` | `id` |
| `title` | `nombre` |
| `stock` | `cantidad` |

**Cliente HTTP nativo de Java** (paquete `java.net.http`, disponible desde JDK 11):

```java
HttpClient cliente = HttpClient.newHttpClient();

HttpRequest solicitud = HttpRequest.newBuilder()
        .uri(URI.create("https://..."))
        .header("Accept", "application/json")
        .GET()
        .build();

HttpResponse<String> respuesta =
        cliente.send(solicitud, HttpResponse.BodyHandlers.ofString());

int codigo = respuesta.statusCode();   // 200 = OK
String cuerpo = respuesta.body();      // texto JSON
```

`cliente.send(...)` lanza `IOException` (falla de red) e `InterruptedException` (el hilo fue interrumpido).

| Código HTTP | Significado |
|---|---|
| 200 | OK: la respuesta trae los datos |
| 404 | Not Found: la ruta no existe |
| 500 | Internal Server Error: falla del servidor |

---

## Estructura esperada al finalizar

```
src/main/java/sv/edu/utec/
|-- Main.java                          <- MODIFICAR (enunciado 4)
|-- api/                               <- NUEVO paquete
|   |-- ProductoApi.java               <- enunciado 1
|   |-- RespuestaProductos.java        <- enunciado 1
|   |-- ProveedorAPI.java              <- enunciado 2
|-- servicio/
|   |-- InventarioJsonService.java     (sin cambios)
|   |-- SincronizacionService.java     <- enunciado 3
|-- datos/                             (sin cambios)
|-- modelo/Producto.java               (sin cambios)
```

---

## Enunciado 1 — Clases de transferencia para el JSON del proveedor (12 %)

En el nuevo paquete `sv.edu.utec.api`:

1. Cree `ProductoApi` con los atributos privados `id` (int), `title` (String) y `stock` (int), constructor sin argumentos, y getters y setters.
2. Cree `RespuestaProductos` con el atributo privado `products` (`List<ProductoApi>`), constructor sin argumentos, y getter y setter.
3. Anote **ambas** clases para que Jackson ignore los campos que no declaran (`total`, `skip`, `limit` y cualquier otro).
4. Agregue a `ProductoApi` el método `public Producto aProducto()`, que devuelva un `Producto` con la correspondencia de la tabla anterior. La columna `nombre` es `VARCHAR(50)`: si `title` supera 50 caracteres, recórtelo.

> **Restricción:** **no** modifique la clase `Producto` con `@JsonProperty`. Si lo hace, el respaldo `inventario.json`, que usa las claves `nombre` y `cantidad`, dejará de funcionar.

## Enunciado 2 — Cliente del proveedor: `ProveedorAPI` (15 %)

En el paquete `sv.edu.utec.api`, cree la clase `ProveedorAPI` con el método:

```java
public List<Producto> obtenerProductos(int limite) throws IOException, InterruptedException;
```

El método debe:

1. Construir la URL con el parámetro `limite` (`/products?limit=<limite>&select=title,stock`) y enviar una solicitud **GET** con `HttpClient`.
2. Validar el código de estado: si **no** es 200, lanzar `IOException` con un mensaje que incluya el código recibido.
3. Deserializar el cuerpo a `RespuestaProductos` con `ObjectMapper`.
4. Convertir cada `ProductoApi` con `aProducto()` y devolver la lista de `Producto`.

## Enunciado 3 — Sincronización idempotente: `SincronizacionService` (15 %)

En el paquete `sv.edu.utec.servicio`, cree la clase `SincronizacionService`:

1. Debe recibir por constructor un `ProveedorAPI` y un `ProductoDAO`.
2. Debe tener el método `sincronizar(int limite)`, que obtenga los productos del proveedor y, **por cada uno**:
   - si ya existe en la base de datos (`dao.existe`), lo **actualice** (`dao.actualizar`);
   - si no existe, lo **inserte** (`dao.insertar`).
3. Debe devolver **cuántos productos se insertaron y cuántos se actualizaron**, por ejemplo con un `int[]` de dos posiciones.
4. Debe ser **idempotente**: ejecutarlo dos veces seguidas no duplica registros ni lanza errores de clave primaria.

> **Restricción:** `ProductoDAO` **no** se modifica.

## Enunciado 4 — Integración en `Main` (8 %)

1. Al final del flujo actual de `Main`, invoque `sincronizar(10)` y muestre el resumen, por ejemplo: `Sincronizacion con la API -> insertados: 8 | actualizados: 2`.
2. Imprima el inventario resultante con el método `imprimir(...)` que ya existe.
3. Capture `InterruptedException` además de `SQLException` e `IOException`. Ningún `catch` debe quedar vacío.
4. `Main` solo coordina: no debe contener `HttpClient` ni `ObjectMapper`.

**Resultado esperado** (primera ejecución, con la base de datos recién creada):

```
Sincronizacion con la API -> insertados: 8 | actualizados: 2

--- Inventario sincronizado ---
ID    PRODUCTO                    CANTIDAD
1     Essence Mascara Lash Princess         99
2     Eyeshadow Palette with Mirror         34
3     Powder Canister                   89
...
10    Gucci Bloom Eau de                91
```

En la **segunda ejecución** debe mostrarse `insertados: 0 | actualizados: 10`. Esta es la prueba de idempotencia.

## Enunciado 5 — Control de versiones y entrega (10 %)

1. Trabaje en la rama `feat/consumo-api` y haga **al menos 3 commits** con mensajes descriptivos, uno por avance. Por ejemplo:

```bash
git add src/main/java/sv/edu/utec/api/ProductoApi.java
git add src/main/java/sv/edu/utec/api/RespuestaProductos.java
git commit -m "feat: DTO para el JSON del proveedor"

git add src/main/java/sv/edu/utec/api/ProveedorAPI.java
git commit -m "feat: cliente HTTP para consumir la API del proveedor"

git add .
git commit -m "feat: sincronizacion idempotente e integracion en Main"
```

2. Complete la sección **«Parcial 2 — Consumo de API»** que está al final de este `README.md` con:
   - su nombre y carnet;
   - la salida de consola de la **segunda** ejecución;
   - el apartado **«Uso de inteligencia artificial»**, donde declare si utilizó IA y para qué.

   Luego haga el commit: `git add README.md` y `git commit -m "docs: evidencia del parcial 2"`.

3. Integre la rama en `main` y suba **ambas** ramas:

```bash
git switch main
git merge feat/consumo-api
git push -u origin feat/consumo-api
git push origin main
```

4. Abra su repositorio en el navegador y verifique que se ven el paquete `api`, sus commits y el README.
5. **Copie el enlace** (`https://github.com/<su-usuario>/ClienteInventario-P2-<carnet>`) y péguelo en la tarea **«Parcial 2 — Enlace del repositorio»** de Moodle antes de la hora límite.

> Solo se evaluará el código que esté en GitHub. Los commits posteriores a la hora límite no se toman en cuenta.

---

## Rúbrica de la parte práctica (60 %)

| Criterio | Evidencia que se revisa | % |
|---|---|---|
| **E1.** DTO y mapeo JSON | `ProductoApi` y `RespuestaProductos` con constructor vacío, accesores y `@JsonIgnoreProperties(ignoreUnknown = true)`; `aProducto()` correcto y con recorte a 50 caracteres; `Producto` sin cambios | 12 % |
| **E2.** Consumo de la API | `HttpClient`/`HttpRequest` GET con URL parametrizada; validación del código 200 con `IOException`; deserialización con `ObjectMapper`; devuelve `List<Producto>` | 15 % |
| **E3.** Sincronización idempotente | Dependencias recibidas por constructor; insertar o actualizar según `existe`; devuelve insertados y actualizados; la segunda ejecución no duplica registros | 15 % |
| **E4.** Integración en `Main` | Invoca el servicio y muestra el resumen y el inventario; captura `SQLException`, `IOException` e `InterruptedException` sin `catch` vacíos; sin lógica HTTP ni JSON en `Main` | 8 % |
| **E5.** Git y entrega | Remoto propio, rama `feat/consumo-api`, al menos 3 commits descriptivos, merge a `main`, push de ambas ramas, README con evidencia y declaración de IA, enlace en Moodle | 10 % |
| **Total** | | **60 %** |

**Penalizaciones:** si el proyecto no compila, E2 a E4 valen como máximo la mitad. Si el enlace no abre o el repositorio está vacío, la parte práctica vale 0. El código que el estudiante no pueda explicar se trata según el reglamento de integridad académica.

---

## Errores frecuentes

| Mensaje o síntoma | Causa y solución |
|---|---|
| `UnrecognizedPropertyException: "total"` | Falta `@JsonIgnoreProperties(ignoreUnknown = true)` en `RespuestaProductos` o en `ProductoApi`. |
| Los productos llegan con `nombre = null` | Se deserializó directamente a `Producto`. Use el DTO y `aProducto()`. |
| `ConnectException` o `UnknownHostException` | No hay internet o la URL está mal escrita. Pruebe el endpoint en el navegador. |
| `JdbcSQLNonTransientConnectionException: Database may be already in use` | La herramienta Database de IntelliJ tiene abierta `inventario.mv.db`. Desconéctela y vuelva a ejecutar. |
| `Unique index or primary key violation` | Se insertó sin verificar `existe`. Revise el enunciado 3. |
| `Value too long for column "NOMBRE"` | Falta recortar `title` a 50 caracteres en `aProducto()`. |
| `remote: Repository not found` al hacer push | La URL de `set-url` es incorrecta o el repositorio no existe. Verifique con `git remote -v`. |
| `Authentication failed` | GitHub no acepta contraseñas desde la terminal. Use un token personal o inicie sesión en IntelliJ (**Settings › Version Control › GitHub**). |
| `! [rejected] main -> main (fetch first)` | Creó el repositorio con README. Ejecute `git pull origin main --allow-unrelated-histories` y repita el push. |

---

## Parcial 2 — Consumo de API (completar por el estudiante)

**Nombre:** Rene Daniel Ventura Sibrian · **Carnet:** 27-0417-2025

### Salida de consola (segunda ejecución)

```
Sincronizacion con la API -> insertados: 0 | actualizados: 10

--- Inventario sincronizado ---
ID    PRODUCTO                    CANTIDAD
1     Essence Mascara Lash Princess         99
2     Eyeshadow Palette with Mirror         34
3     Powder Canister                   89
4     Red Lipstick                      91
5     Red Nail Polish                   79
6     Calvin Klein CK One               29
7     Chanel Coco Noir Eau De           58
8     Dior J'adore                      98
9     Dolce Shine Eau de                 4
10    Gucci Bloom Eau de                91

Process finished with exit code 0

```

### Uso de inteligencia artificial


Se utilizó inteligencia artificial, específicamente ChatGPT, como herramienta de apoyo durante el desarrollo del proyecto para resolver dudas relacionadas con sintaxis, buenas prácticas de programación y el uso de anotaciones de Jackson, como @JsonIgnoreProperties. También se utilizó como referencia para el manejo de excepciones en la clase Main y para consultar comandos de Git relacionados con la creación de ramas y la realización de merges.

La lógica de sincronización idempotente fue diseñada, implementada y probada por mí. Asimismo, revisé y comprendí el código utilizado durante el desarrollo, por lo que puedo explicar su funcionamiento y las decisiones tomadas en su implementación.
