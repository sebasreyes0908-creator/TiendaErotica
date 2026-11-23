# Velvet Boutique: CRUD con JavaFX

Aplicación de escritorio para manejar el inventario de una tienda para adultos. Permite **crear, consultar, editar y eliminar productos** desde una interfaz con tema oscuro, tarjetas con imagen, filtros y estadísticas en vivo. Los datos se guardan en un archivo binario usando **serialización de objetos en Java**, sin necesidad de base de datos.

## Tecnologías

| Herramienta | Uso |
|---|---|
| Java 17+ | Lenguaje (usa `record`, `switch` moderno y *pattern matching*) |
| JavaFX 21 | Interfaz gráfica |
| Maven (con Maven Wrapper) | Compilación y ejecución, sin instalar Maven |
| CSS de JavaFX | Estilos del tema oscuro (`estilos.css`) |
| Serialización (`ObjectOutputStream`) | Persistencia en `productos.dat` |

## Funcionalidades principales

### CRUD de productos
- **Crear:** botón *Nuevo producto* (o `Ctrl+N`) que abre un panel lateral con el formulario.
- **Consultar:** los productos se muestran en una cuadrícula de tarjetas con imagen, categoría, marca, precio y estado del stock.
- **Editar:** clic en una tarjeta para abrir el mismo panel con los datos cargados.
- **Eliminar:** pide confirmación en un diálogo propio y luego permite **Deshacer** durante unos segundos.

### Datos de cada producto
Código (único), nombre, categoría, marca, precio, stock, opción de *empaque discreto* e imagen.

Categorías disponibles: Juguetes, Lencería, Lubricantes, Aceites de masaje, Juegos de pareja, Preservativos y Accesorios. Cada una tiene sus propios colores.

### Validaciones
- El código, el nombre y la categoría son obligatorios.
- El precio debe ser mayor que 0 y el stock no puede ser negativo.
- No se permiten dos productos con el mismo código.
- Si la marca queda vacía, se guarda como *Genérica*.
- Los errores se muestran como notificaciones en pantalla.

### Control de stock
- Botones **−** y **+** en cada tarjeta para ajustar el stock al instante.
- Etiqueta de color: verde (stock normal), naranja (stock bajo, 3 unidades o menos) y *Agotado*.
- Alertas automáticas cuando un producto queda con stock bajo o agotado.
- Menú de **clic derecho** con *Editar*, *Sumar 5 unidades* y *Eliminar*.

### Imágenes
- Se agregan con un clic o **arrastrando y soltando** un archivo (png, jpg, gif o bmp).
- La imagen se copia a la carpeta `imagenes/`, así el producto no la pierde si se borra la original.
- Si el producto no tiene foto, se genera una imagen con sus iniciales y los colores de la categoría.

### Búsqueda, filtros y orden
- Búsqueda en vivo por texto (`Ctrl+F`).
- Filtro por categoría, con el número de productos de cada una.
- Botón **Por reabastecer** que muestra solo los productos agotados o con stock bajo.
- Orden por nombre, precio (menor/mayor) o stock.

### Estadísticas
En la parte superior se muestran, con números animados:
- Total de productos.
- Unidades en inventario.
- Valor total del inventario (precio × stock, en pesos).
- Productos por reabastecer.

### Detalles de la interfaz
- Las tarjetas se elevan y la imagen hace zoom al pasar el mouse.
- Notificaciones tipo *toast* que se cierran solas y se pausan con el mouse encima.
- Diálogo de confirmación con fondo desenfocado.
- Atajos: `Ctrl+N` (nuevo), `Ctrl+F` (buscar), `Esc` (cerrar panel o diálogo).

## Cómo ejecutarlo

Requisito: **JDK 17 o superior**. No hace falta instalar Maven, porque el proyecto trae el Maven Wrapper.

En Windows (CMD), desde la carpeta del proyecto:

```
mvnw.cmd clean javafx:run
```

En Linux o macOS:

```
./mvnw clean javafx:run
```

En IntelliJ también se puede abrir el `pom.xml` como proyecto y ejecutar **Plugins → javafx → javafx:run** desde la pestaña Maven.

La primera vez (cuando todavía no existe `productos.dat`) el programa carga 8 productos de ejemplo. Para empezar de cero, borra `productos.dat` y la carpeta `imagenes/`.

## Estructura del proyecto

El código sigue una arquitectura por capas:

```
src/main/java/co/tienda/
├── modelo/
│   ├── Producto.java          ← record Serializable, validaciones y "withers" (conStock, conImagen)
│   └── Categoria.java         ← enum con nombre y colores de cada categoría
├── persistencia/
│   ├── ArchivoProductos.java  ← guardar/cargar con ObjectOutputStream / ObjectInputStream
│   └── GestorImagenes.java    ← copia las imágenes a la carpeta imagenes/
├── servicio/
│   ├── ProductoServicio.java  ← Create, Read, Update, Delete + estadísticas
│   └── DatosEjemplo.java      ← productos iniciales de ejemplo
└── vista/
    ├── App.java               ← ventana principal, filtros y acciones del CRUD
    ├── TarjetaProducto.java   ← tarjeta con imagen, hover, botones +/- y menú contextual
    ├── PanelEdicion.java      ← panel lateral del formulario (con arrastrar y soltar imagen)
    ├── Notificaciones.java    ← alertas tipo "toast" animadas
    ├── Dialogos.java          ← diálogo de confirmación con desenfoque
    ├── Imagenes.java          ← recorte de imágenes e imagen por defecto
    └── Iconos.java            ← íconos SVG
```

## Conceptos clave

- **El record es inmutable.** No tiene *setters*; para cambiar el stock se crea un producto nuevo con `p.conStock(n)` y se reemplaza en la lista.
- **Serialización de records.** Al leer el archivo, Java usa el constructor canónico, así que las validaciones también se aplican a los datos que vienen del archivo.
- **Las imágenes no se serializan.** En el record solo se guarda el nombre del archivo, para que `productos.dat` se mantenga pequeño.
- **Escritura segura.** Cada cambio escribe la lista completa en un archivo `.tmp` y luego reemplaza el original, para que no se dañe si algo falla a mitad de la escritura.
